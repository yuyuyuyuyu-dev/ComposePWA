package dev.yuyuyuyuyu.composepwa

internal fun matchesGlob(
    path: String,
    pattern: String,
): Boolean {
    val pathSegments = path.split('/').filter { it.isNotEmpty() }

    return expandBraces(pattern.replace('\\', '/')).any { expandedPattern ->
        matchesSegments(pathSegments, expandedPattern.split('/').filter { it.isNotEmpty() })
    }
}

private fun matchesSegments(
    pathSegments: List<String>,
    patternSegments: List<String>,
): Boolean =
    when {
        patternSegments.isEmpty() -> {
            pathSegments.isEmpty()
        }

        patternSegments.first() == "**" -> {
            (0..pathSegments.size).any { consumed ->
                pathSegments.take(consumed).none { it.startsWith('.') } &&
                    matchesSegments(pathSegments.drop(consumed), patternSegments.drop(1))
            }
        }

        else -> {
            pathSegments.isNotEmpty() &&
                matchesSegment(pathSegments.first(), patternSegments.first()) &&
                matchesSegments(pathSegments.drop(1), patternSegments.drop(1))
        }
    }

private fun matchesSegment(
    name: String,
    pattern: String,
): Boolean = (!name.startsWith('.') || pattern.startsWith('.')) && globSegmentToRegex(pattern).matches(name)

private fun globSegmentToRegex(pattern: String): Regex {
    val regex = StringBuilder()
    var index = 0
    while (index < pattern.length) {
        val char = pattern[index]
        val classEnd = if (char == '[') pattern.indexOf(']', index + 2) else -1
        when {
            char == '*' -> {
                regex.append("[^/]*")
            }

            char == '?' -> {
                regex.append("[^/]")
            }

            classEnd != -1 -> {
                val body = pattern.substring(index + 1, classEnd)
                val negated = body.startsWith('!') || body.startsWith('^')
                val members = if (negated) body.drop(1) else body
                regex.append(if (negated) "[^" else "[")
                regex.append(members.replace("\\", "\\\\").replace("[", "\\["))
                regex.append(']')
                index = classEnd
            }

            else -> {
                regex.append(Regex.escape(char.toString()))
            }
        }
        index++
    }
    return Regex(regex.toString())
}

private fun expandBraces(pattern: String): List<String> {
    val braces = findBraces(pattern) ?: return listOf(pattern)
    val prefix = pattern.substring(0, braces.first)
    val body = pattern.substring(braces.first + 1, braces.last)
    val suffix = pattern.substring(braces.last + 1)
    val alternatives = splitAlternatives(body) ?: expandSequence(body)

    return alternatives?.flatMap { expandBraces(prefix + it + suffix) }
        ?: expandBraces(suffix).map { "$prefix{$body}$it" }
}

private fun findBraces(pattern: String): IntRange? {
    val open = pattern.indexOf('{')
    var depth = 0
    val close =
        (maxOf(open, 0) until pattern.length).firstOrNull { index ->
            when (pattern[index]) {
                '{' -> depth++
                '}' -> depth--
            }
            open != -1 && depth == 0
        }

    return close?.let { open..it }
}

private fun splitAlternatives(body: String): List<String>? {
    var depth = 0
    val commas =
        body.indices.filter { index ->
            when (body[index]) {
                '{' -> depth++
                '}' -> depth--
            }
            body[index] == ',' && depth == 0
        }

    return commas
        .takeIf {
            it.isNotEmpty()
        }?.let { (listOf(-1) + it + body.length).zipWithNext { start, end -> body.substring(start + 1, end) } }
}

private fun expandSequence(body: String): List<String>? {
    val numbers = Regex("""^(-?\d+)\.\.(-?\d+)$""").find(body)?.destructured
    val letters = Regex("""^([a-zA-Z])\.\.([a-zA-Z])$""").find(body)?.destructured

    return when {
        numbers != null -> {
            val (start, end) = numbers
            progression(start.toInt(), end.toInt()).map { it.toString() }
        }

        letters != null -> {
            val (start, end) = letters
            progression(start[0].code, end[0].code).map { it.toChar().toString() }
        }

        else -> {
            null
        }
    }
}

private fun progression(
    start: Int,
    end: Int,
): IntProgression = if (start <= end) start..end else start downTo end
