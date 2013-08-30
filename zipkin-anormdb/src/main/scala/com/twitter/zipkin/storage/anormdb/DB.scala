/*
 * Copyright 2013 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.zipkin.storage.anormdb

import anorm._
import anorm.SqlParser._
import java.sql.{Blob, Connection, DriverManager, SQLException}
import com.twitter.util.{Try, Return, Throw}

/**
 * Provides SQL database access via Anorm from the Play framework.
 *
 * See http://www.playframework.com/documentation/2.1.1/ScalaAnorm for
 * documentation on using Anorm.
 */
case class DB(dbconfig: DBConfig = new DBConfig()) {

  // Load the driver
  Class.forName(dbconfig.driver)

  // Install the schema if requested
  if (dbconfig.install) this.install().close()

  /**
   * Gets a java.sql.Connection to the SQL database.
   *
   * Example usage:
   *
   * implicit val conn: Connection = (new DB()).getConnection()
   * // Do database updates
   * conn.close()
   */
  def getConnection() = {
    DriverManager.getConnection(dbconfig.location)
  }

  /**
   * Execute SQL in a transaction.
   *
   * Example usage:
   *
   * db.withTransaction(conn, { implicit conn: Connection =>
   *   // Do database updates
   * })
   */
  def withTransaction[A](conn: Connection, code: Connection => A): Try[A] = {
    val autoCommit = conn.getAutoCommit
    try {
      conn.setAutoCommit(false)
      val result = code(conn)
      conn.commit()
      Return(result)
    }
    catch {
      case e: Throwable => {
        conn.rollback()
        Throw(e)
      }
    }
    finally {
      conn.setAutoCommit(autoCommit)
    }
  }

  /**
   * Set up the database tables.
   *
   * Returns an open database connection, so remember to close it, for example
   * with `(new DB()).install().close()`
   */
  def install(): Connection = {
    implicit val con = this.getConnection()
    SQL(
      """CREATE TABLE IF NOT EXISTS zipkin_spans (
        |  span_id BIGINT NOT NULL,
        |  parent_id BIGINT,
        |  trace_id BIGINT NOT NULL,
        |  span_name VARCHAR(255) NOT NULL,
        |  debug SMALLINT NOT NULL,
        |  duration BIGINT,
        |  created_ts BIGINT
        |)
      """.stripMargin).execute()
    //SQL("CREATE INDEX trace_id ON zipkin_spans (trace_id)").execute()
    SQL(
      """CREATE TABLE IF NOT EXISTS zipkin_annotations (
        |  span_id BIGINT NOT NULL,
        |  trace_id BIGINT NOT NULL,
        |  span_name VARCHAR(255) NOT NULL,
        |  service_name VARCHAR(255) NOT NULL,
        |  value TEXT,
        |  ipv4 INT,
        |  port INT,
        |  a_timestamp BIGINT NOT NULL,
        |  duration BIGINT
        |)
      """.stripMargin).execute()
    //SQL("CREATE INDEX trace_id ON zipkin_annotations (trace_id)").execute()
    SQL(
      """CREATE TABLE IF NOT EXISTS zipkin_binary_annotations (
        |  span_id BIGINT NOT NULL,
        |  trace_id BIGINT NOT NULL,
        |  span_name VARCHAR(255) NOT NULL,
        |  service_name VARCHAR(255) NOT NULL,
        |  annotation_key VARCHAR(255) NOT NULL,
        |  annotation_value %s,
        |  annotation_type_value INT NOT NULL,
        |  ipv4 INT,
        |  port INT
        |)
      """.stripMargin.format(this.getBlobType)).execute()
    //SQL("CREATE INDEX trace_id ON zipkin_binary_annotations (trace_id)").execute()
    SQL(
      """CREATE TABLE IF NOT EXISTS zipkin_dependencies (
        |  dlid %s,
        |  start_ts BIGINT NOT NULL,
        |  end_ts BIGINT NOT NULL
        |)
      """.stripMargin.format(this.getAutoIncrement)).execute()
    SQL(
      """CREATE TABLE IF NOT EXISTS zipkin_dependency_links (
        |  dlid BIGINT NOT NULL,
        |  parent VARCHAR(255) NOT NULL,
        |  child VARCHAR(255) NOT NULL,
        |  m0 BIGINT NOT NULL,
        |  m1 DOUBLE PRECISION NOT NULL,
        |  m2 DOUBLE PRECISION NOT NULL,
        |  m3 DOUBLE PRECISION NOT NULL,
        |  m4 DOUBLE PRECISION NOT NULL
        |)
      """.stripMargin).execute()
    con
  }

  // Get the column the current database type uses for BLOBs.
  private def getBlobType = dbconfig.description match {
    case "PostgreSQL" => "BYTEA" /* As usual PostgreSQL has to be different */
    case "MySQL" => "MEDIUMBLOB" /* MySQL has length limits, in this case 16MB */
    case _ => "BLOB"
  }

  private def getAutoIncrement = dbconfig.description match {
    case "SQLite in-memory" => "INTEGER PRIMARY KEY AUTOINCREMENT" // Must be nullable
    case "SQLite persistent" => "INTEGER PRIMARY KEY AUTOINCREMENT"
    case "H2 in-memory" => "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY"
    case "H2 persistent" => "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY"
    case "PostgreSQL" => "BIGSERIAL PRIMARY KEY"
    case "MySQL" => "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY"
  }

  // (Below) Provide Anorm with the ability to handle BLOBs.
  // The documentation says it can do it in 2.1.1, but it's wrong.

  /**
   * Attempt to convert a SQL value into a byte array.
   */
  private def valueToByteArrayOption(value: Any): Option[Array[Byte]] = {
    value match {
      case bytes: Array[Byte] => Some(bytes)
      case blob: Blob => try {
          Some(blob.getBytes(1, blob.length.asInstanceOf[Int]))
        }
        catch {
          case e: SQLException => None
        }
      case _ => None
    }
  }

  /**
   * Implicitly convert an Anorm row to a byte array.
   */
  def rowToByteArray: Column[Array[Byte]] = {
    Column.nonNull[Array[Byte]] { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      valueToByteArrayOption(value) match {
        case Some(bytes) => Right(bytes)
        case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Byte Array for column " + qualified))
      }
    }
  }

  /**
   * Build a RowParser factory for a byte array column.
   */
  def bytes(columnName: String): RowParser[Array[Byte]] = {
    get[Array[Byte]](columnName)(rowToByteArray)
  }
}
