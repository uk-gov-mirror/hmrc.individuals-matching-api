/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.individualsmatchingapi.services

import com.mongodb.ErrorCategory
import org.bson.BsonType
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{MongoCollection, MongoWriteException, SingleObservableFuture}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent

import java.util.Date
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateNinoMatchService @Inject() (
  mongoComponent: MongoComponent
)(implicit val ec: ExecutionContext)
    extends Logging {

  private val collection: MongoCollection[Document] =
    mongoComponent.database.getCollection("ninoMatch")

  private val lockCollection: MongoCollection[Document] =
    mongoComponent.database.getCollection("locks")

  private val lockId = "updateNinoMatch"

  // Trigger at the time of Startup
  updateItem()

  private def acquireLock(): Future[Boolean] = {
    val lockDoc = Document("_id" -> lockId, "createdAt" -> new Date())
    lockCollection.insertOne(lockDoc).toFuture().map(_ => true).recover {
      case ex: MongoWriteException if ex.getError.getCategory == ErrorCategory.DUPLICATE_KEY =>
        logger.info("Lock already exists. Skipping cron job.")
        false
      case ex =>
        logger.error("Unexpected error while acquiring lock", ex)
        false
    }
  }

  private def updateItem(): Future[Unit] =
    acquireLock().flatMap {
      case true =>
        logger.info("Lock acquired. Starting update...")

        collection
          .updateMany(
            Filters.`type`(
              "createdAt",
              BsonType.STRING
            ),
            List(
              Document(
                "$set" -> Document(
                  "createdAt" -> Document("$toDate" -> "$createdAt")
                )
              )
            )
          )
          .toFuture()
          .map { result =>
            logger.info(s"Update completed: ${result.getModifiedCount} documents updated.")
          }
          .recover { case ex =>
            logger.error("Update failed.", ex)
          }

      case false =>
        Future.successful(())
    }

}
