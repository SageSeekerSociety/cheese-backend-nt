# Valuable Experience

Fixing bugs can be time consuming. Luckily, we have learned some valuable experience from
that disgusting process. I hope they are helpful to you.

## 1. Do not use kotlin `open class` in JPA entity

`@ManyToOne(fetch = FetchType.LAZY)` is a useful feature in JPA which can improve performance
by querying the related entity only when it's field other than id is accessed. However, if you
use `open class` in kotlin, the lazy loading will not work as expected. The reason is that
kotlin `open class` is not final, so the lazy loading proxy cannot be created.

See: https://stackoverflow.com/questions/62116317/why-does-fetchtype-lazy-not-work-in-hibernate-jpa