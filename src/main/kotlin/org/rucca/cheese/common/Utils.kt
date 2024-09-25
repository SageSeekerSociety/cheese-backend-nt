package org.rucca.cheese.common

fun <T, K> List<T>.groupConsecutiveBy(keySelector: (T) -> K): List<List<T>> {
    if (isEmpty()) return emptyList()

    val result = mutableListOf<MutableList<T>>()
    var currentGroup = mutableListOf(this.first())
    var currentKey = keySelector(this.first())

    for (item in this.drop(1)) {
        val key = keySelector(item)
        if (key == currentKey) {
            currentGroup.add(item)
        } else {
            result.add(currentGroup)
            currentGroup = mutableListOf(item)
            currentKey = key
        }
    }
    result.add(currentGroup)

    return result
}
