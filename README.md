# KStreamlined Backend

GraphQL API for the [KStreamlined mobile apps](https://github.com/ReactiveCircus/kstreamlined-mobile).

The API combines multiple Kotlin-related RSS feeds into a single list of `FeedEntry` and supports filtering by feed sources.

Currently supported feed sources:

1. [Kotlin Blog](https://blog.jetbrains.com/kotlin/feed/) - Latest news from the official Kotlin Blog
2. [Kotlin YouTube Channel](https://www.youtube.com/feeds/videos.xml?channel_id=UCP7uiEZIqci43m22KDl0sNw) - The official YouTube channel of the Kotlin programming language
3. [Talking Kotlin](https://talkingkotlin.com/feed) - Technical show discussing everything Kotlin, hosted by Hadi and Sebastian
4. [Kotlin Weekly](https://rss.app/feeds/QKefltG2nWU9PWuu.xml) - Weekly community Kotlin newsletter, hosted by Enrique

## Tech

- [Spring Boot](https://github.com/spring-projects/spring-boot)
- [Netflix DGS Framework](https://netflix.github.io/dgs/)
- [Ktor (client)](https://ktor.io/)
- [GraalVM](https://www.graalvm.org/)

## Building and running locally

To compile the project:

```
./gradlew assemble
```

To run all checks (including both unit tests and static analysis):

```
./gradlew check
```

To run the service as a Spring Boot application:

```
./gradlew bootRun
```

The GraphQL playground is available at:

```
http://localhost:8000/graphiql
```

To assemble an executable jar archive:

```
./gradlew bootJar
```

## GraalVM Native Image

Make sure the required version of GraalVM JDK is installed.

To compile a native executable:

```
./gradlew nativeCompile --no-configuration-cache
```

To run the native executable:

```
./gradlew nativeRun --no-configuration-cache
```

## License

```
Copyright 2022 Yang Chen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
