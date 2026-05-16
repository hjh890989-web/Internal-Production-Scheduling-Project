---
name: 302-jpa-querydsl-dynamic-query-rules
description: Defines conventions for using dynamic queries with JPA + QueryDSL PredicateBuilder.
---
Globs: src/main/java/**/*Repository*, src/main/java/**/*QueryDsl*

# JPA + QueryDSL Dynamic Query Rules
- Use BooleanBuilder or Predicate combinators
- Apply null-safe parameter binding (ifPresent, Optional)
- Match method naming: findBy*, searchBy*, listBy*
- Avoid redundant joins in reusable base queries
- Use typed predicates instead of raw expressions
- Create query factory beans if needed
