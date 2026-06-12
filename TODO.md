# TODO - OpenSearch client migration

## Phase 1: Prep & Dependency
- [x] Add `org.opensearch.client:opensearch-java` dependency to `services/game-service/pom.xml`
- [x] Decide on a concrete `opensearch-java` version (2.x.x) in `pom.xml`


## Phase 2: Implement typed OpenSearch client
- [x] Add `opensearch-java` dependency (done)
- [ ] Create an OpenSearch client bean (scheme/host/port/basic-auth)
- [ ] Replace `RestTemplate` calls for index create/delete and search requests


## Phase 3: Refactor `OpenSearchService`
- [ ] Replace raw JSON query building with `opensearch-java` query builders:
  - [ ] multi_match (field boosts, fuzziness, prefix_length)
  - [ ] bool (must/filter)
  - [ ] range (price)
  - [ ] more_like_this
  - [ ] completion suggester
- [ ] Replace response parsing with typed response extraction for IDs/total

## Phase 4: Verification
- [ ] Build the module: `mvn -pl services/game-service test`
- [ ] Smoke test key endpoints for search + suggestions + similar games

