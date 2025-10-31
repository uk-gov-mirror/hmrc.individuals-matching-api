/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.individualsmatchingapi.domain

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import uk.gov.hmrc.domain.Nino

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

case class NinoMatch(nino: Nino, id: UUID, createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC))

object NinoMatch {
  implicit val format: Format[NinoMatch] = (
    (__ \ "nino").format[Nino] and
      (__ \ "id").format[UUID] and
      (__ \ "createdAt").format(using localDateTimeFormat)
  )(NinoMatch.apply, nm => (nm.nino, nm.id, nm.createdAt))
}

private val localDateTimeFormat: Format[LocalDateTime] = Format(
  (__ \ "$date" \ "$numberLong").read[String].map { millis =>
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis.toLong), ZoneOffset.UTC)
  },
  (dt: LocalDateTime) =>
    Json.obj(
      "$date" -> Json.obj(
        "$numberLong" -> dt.toInstant(ZoneOffset.UTC).toEpochMilli.toString
      )
    )
)
