# Checklist for a contribution to openhab-addons

The bundle follows the layout of openhab-addons and builds there with all checks
(`.github/workflows/build.yml`). Open points before a pull request:

- [x] All classes annotated with `@NonNullByDefault`, nullable fields marked `@Nullable`.
- [x] The shared Jetty `HttpClient` from `HttpClientFactory` is used.
- [x] JSON with Gson and DTO classes, no own parser.
- [x] Thing status texts in English with `@text/` keys, German translation in `blueriiot_de.properties`.
- [x] Unit tests for the AWS signature and the parsing of (anonymized) cloud responses.
- [ ] Generate the i18n file with `mvn i18n:generate-default-translations` and compare it with
      `OH-INF/i18n/blueriiot.properties`.
- [ ] Add the bundle to `bundles/pom.xml` and `CODEOWNERS` of openhab-addons, version of the target branch in `pom.xml`.
- [ ] Commits signed off with the real name (`git commit -s`, Developer Certificate of Origin).
- [ ] Mention in the PR that the API is unofficial (same interface as the Blue Connect app).
- [ ] The Main UI page in `examples/conf/ui` is not part of an add-on; offer it separately if wanted.
