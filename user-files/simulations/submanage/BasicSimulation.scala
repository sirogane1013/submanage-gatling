/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package submanage

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class BasicSimulation extends Simulation {

  val httpProtocol = http
    // Here is the root for all relative URLs
    .baseUrl("http://127.0.0.1")
    // Here are the common headers
    .acceptHeader("application/json, text/plain, */*")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("ja,en-US;q=0.9,en;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val $token = "${csrf_token(0)}"
  val $session = "${submanage_session(0)}"

  val userFeeder = Iterator.from(0).map(i => Map(
    "email" -> s"test$i@example.com",
    "password" -> "password",
  ))

  // A scenario is a chain of requests and pauses
  val scn = scenario("Scenario Name")
    .feed(userFeeder)
    .exec(
      http("GET /login")
        .get("/login")
    )
    .pause(10)
    .exec(
      http("GET /sanctum/csrf-cookie")
        .get("/sanctum/csrf-cookie")
          .check(headerRegex("Set-Cookie", "XSRF-TOKEN=(.*)%3D;").findAll.saveAs("csrf_token"))
          .check(headerRegex("Set-Cookie", "submanage_session=(.*);").findAll.saveAs("submanage_session"))
    )
    .exec(
      http("POST /api/user/login")
        .post("/api/user/login")
        .header("X-XSRF-TOKEN", $token)
          .formParam("email", "${email}")
          .formParam("password", "${password}")
            .check(headerRegex("Set-Cookie", "XSRF-TOKEN=(.*)%3D;").findAll.saveAs("csrf_token"))
            .check(headerRegex("Set-Cookie", "submanage_session=(.*);").findAll.saveAs("submanage_session"))
    )
    .pause(5)
    .exec(
      http("GET /api/service/list")
        .get("/api/service/list")
        .header("X-XSRF-TOKEN", $token)
          .check(headerRegex("Set-Cookie", "XSRF-TOKEN=(.*)%3D;").findAll.saveAs("csrf_token"))
          .check(headerRegex("Set-Cookie", "submanage_session=(.*);").findAll.saveAs("submanage_session"))
    )
    .pause(10)
    .exec(
      http("POST /api/service/add")
        .post("/api/service/add")
        .header("X-XSRF-TOKEN", $token)
          .formParam("name", "gatling_test")
          .formParam("category_id", "1")
          .formParam("price", "100")
            .check(headerRegex("Set-Cookie", "XSRF-TOKEN=(.*)%3D;").findAll.saveAs("csrf_token"))
            .check(headerRegex("Set-Cookie", "submanage_session=(.*);").findAll.saveAs("submanage_session"))
    )
    
  setUp(
    scn.inject(
      atOnceUsers(1),
      // constantUsersPerSec(10).during(5.seconds),
    ).protocols(httpProtocol)
  )
}
