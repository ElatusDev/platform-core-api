# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

### Fixed
- **build**: Configure Lombok annotation processor for maven-compiler-plugin 3.14.0 — processors are
  no longer auto-discovered from the classpath in this version (`86b8f72`)
- **build**: Add missing Lombok dependency to utilities module (`86b8f72`)

### Changed
- **docs**: Rename `CLAUDE.md` → `AI-CODE-REF.md` to avoid collision with Claude Code convention (`d27b57a`)
- **docs**: Rename `ARCHITECTURE.md` → `DESIGN.md` and `AI-code-ref.md` → `CLAUDE.md` (now `AI-CODE-REF.md`)
  with updated cross-references (`03569de`)

### Added
- **docs**: Add root `CLAUDE.md` as lightweight Claude Code entry point
- **docs**: Add `SECURITY.md` for responsible vulnerability disclosure
- **docs**: Add `CHANGELOG.md` following Keep a Changelog format
- **docs**: Add `CONTRIBUTING.md` with onboarding guide and workflow
- **docs**: Add `docs/adr/` with Architecture Decision Records
