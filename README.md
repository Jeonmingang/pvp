# PvPCompetition v1.0.1

## Build
```
mvn -B -DskipTests package
```

## Troubleshooting
- **actions/upload-artifact deprecation**: use `@v4` (already in `.github/workflows/main.yml`).
- **Invalid workflow**: ensure the YAML is at `.github/workflows/main.yml` _not_ `pom.xml` or other XML.
- **Java version**: must be Java 8 (Temurin). In GitHub Actions we set it via `actions/setup-java@v4`.
- **Repository**: Spigot API snapshot repo is included in `pom.xml`.
- **Encoding**: UTF-8 set to avoid Korean text issues.
