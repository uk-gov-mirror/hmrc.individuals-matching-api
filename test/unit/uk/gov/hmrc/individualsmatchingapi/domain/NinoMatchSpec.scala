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

package unit.uk.gov.hmrc.individualsmatchingapi.domain

import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.NinoMatch

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

class NinoMatchSpec extends AnyWordSpec with Matchers {

  "ModifiedDetails JSON format" should {

    val validNino = "CS700100A"
    val nino = Nino(validNino)
    val uuid = UUID.randomUUID()

    "serialize ModifiedDetails to JSON correctly" in {
      val createdAt = LocalDateTime.of(2023, 5, 10, 12, 30)

      val ninoMatch = NinoMatch(nino, uuid, createdAt)

      val json = Json.toJson(ninoMatch)

      val expectedJson = Json.obj(
        "nino" -> nino.toString,
        "id"   -> uuid.toString,
        "createdAt" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> createdAt.toInstant(ZoneOffset.UTC).toEpochMilli.toString
          )
        )
      )

      json shouldBe expectedJson
    }

    "deserialize JSON to ModifiedDetails correctly" in {
      val createdAtMillis = Instant.parse("2023-05-10T12:30:00Z").toEpochMilli.toString

      val json = Json.obj(
        "nino" -> validNino,
        "id"   -> uuid.toString,
        "createdAt" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> createdAtMillis
          )
        )
      )

      val result = json.validate[NinoMatch]

      result.isSuccess shouldBe true
      val ninoMatch = result.get
      ninoMatch.createdAt shouldBe LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAtMillis.toLong), ZoneOffset.UTC)
      ninoMatch.nino shouldBe nino
      ninoMatch.id shouldBe uuid
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj(
        "nino"      -> validNino,
        "id"        -> uuid.toString,
        "createdAt" -> "not a date"
      )

      val result = invalidJson.validate[NinoMatch]
      result.isError shouldBe true
    }
  }
}
