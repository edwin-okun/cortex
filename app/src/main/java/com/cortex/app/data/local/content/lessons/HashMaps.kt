package com.cortex.app.data.local.content.lessons

import com.cortex.app.data.local.content.lesson
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track

val HashMaps = lesson("hash-maps") {
    track = Track.ALGORITHMS
    tier = Tier.FOUNDATIONS
    title = "Hash Maps"

    hook {
        problem = """
            A log-analysis service needs to count occurrences of each error code across
            10 million log events. The naive approach — for each code, scan the full list
            to count matches — is O(n²): 100 trillion operations for 10 M events.
        """.trimIndent()
        stakes = """
            Log aggregation, session deduplication, and cache invalidation all reduce to
            the same primitive: look up a key in O(1), update its value, move on.
            The difference between O(n²) and O(n) is the difference between a query
            that times out and one that finishes before the next event arrives.
        """.trimIndent()
    }

    intuition {
        narration(
            "A hash map stores key-value pairs in an array of buckets. A hash function " +
                "maps each key to a bucket index. Lookup, insert, and delete are O(1) on " +
                "average because computing the index is constant-time arithmetic.",
            "Collisions — two keys mapping to the same bucket — are resolved by chaining " +
                "(a linked list per bucket) or open addressing. With a good hash function " +
                "and a load factor below ~0.75, chains stay short and the O(1) guarantee holds.",
            "The two-sum insight: instead of searching for the complement after the fact, " +
                "store each number as you go. When you see a new number, check whether its " +
                "complement already exists. One pass, O(1) per step.",
        )
        visual = null
    }

    workedExample {
        step("Problem: find indices of two numbers in [2, 7, 11, 15] that sum to 9.")
        step(
            """
            def two_sum(nums, target):
                seen = {}          # value -> index
                for i, n in enumerate(nums):
                    complement = target - n
                    if complement in seen:
                        return [seen[complement], i]
                    seen[n] = i
            """.trimIndent()
        )
        step("i=0, n=2: complement=7. seen={} — not found. seen={2:0}.")
        step("i=1, n=7: complement=2. seen={2:0} — found at index 0. Return [0, 1].")
        step("Total iterations: 2. No nested loop. O(n) time, O(n) space.")
        step("---")
        step("Frequency count: count error codes in ['ERR_404', 'ERR_500', 'ERR_404', 'ERR_404'].")
        step(
            """
            events = ['ERR_404', 'ERR_500', 'ERR_404', 'ERR_404']
            counts = {}
            for code in events:
                counts[code] = counts.get(code, 0) + 1
            # Result: {'ERR_404': 3, 'ERR_500': 1}
            """.trimIndent()
        )
        step("One pass, O(n). Lookup and update at each step is O(1) amortized.")
    }

    fadedPractice {
        problem(id = "hm-p1", scaffold = 2) {
            prompt = "Count the frequency of each character in the string 'deadlock'."
            hint("Iterate over each character. Use a dict: freq[ch] = freq.get(ch, 0) + 1.")
            hint("After the loop, freq should be {'d':2,'e':1,'a':1,'l':1,'o':1,'c':1,'k':1}.")
            answer = """
                freq = {}
                for ch in 'deadlock':
                    freq[ch] = freq.get(ch, 0) + 1
                # {'d':2,'e':1,'a':1,'l':1,'o':1,'c':1,'k':1}
            """.trimIndent()
        }
        problem(id = "hm-p2", scaffold = 1) {
            prompt = "Find the first duplicate value in [4, 3, 2, 7, 8, 2, 3, 1]. Return -1 if none."
            hint("Maintain a seen set. Return the first element already in the set.")
            answer = """
                def first_duplicate(nums):
                    seen = set()
                    for n in nums:
                        if n in seen:
                            return n
                        seen.add(n)
                    return -1
                # first_duplicate([4,3,2,7,8,2,3,1]) -> 2
            """.trimIndent()
        }
        problem(id = "hm-p3", scaffold = 0) {
            prompt = """
                Find all unique pairs in [1, 5, 3, 7, 4, 2] that sum to 8.
                Each pair should appear once; order within a pair does not matter.
            """.trimIndent()
            answer = """
                def pairs_sum(nums, target):
                    seen = set()
                    result = set()
                    for n in nums:
                        complement = target - n
                        if complement in seen:
                            result.add((min(n, complement), max(n, complement)))
                        seen.add(n)
                    return list(result)
                # pairs_sum([1,5,3,7,4,2], 8) -> [(1,7),(3,5)]
            """.trimIndent()
        }
    }

    transferProblem {
        prompt = """
            A data pipeline receives 50 million user-event rows per day. Each row has
            (user_id, event_type, minute_bucket). De-duplicate: emit each distinct
            (user_id, event_type, minute_bucket) tuple exactly once.
            What data structure solves this in the minimum time complexity, and why?
            Compare against a sort-then-dedup approach.
        """.trimIndent()
        solution = """
            Use a hash set: O(n) time, O(d) space where d = distinct tuples.

            seen = set()
            for row in stream:
                key = (row.user_id, row.event_type, row.minute_bucket)
                if key not in seen:
                    seen.add(key)
                    emit(row)

            Each row requires one O(1) set lookup and at most one O(1) insert.
            Total: O(n) time.

            Sort-then-dedup costs O(n log n) for the sort — more expensive and requires
            buffering the entire stream in memory before emitting a single row.
            The hash-set approach can stream: constant working memory beyond the set itself.
        """.trimIndent()
    }

    reviewCards {
        card(
            id = "hm-complexity",
            front = "What is the average-case time complexity of hash map insert and lookup, and what assumption does it rely on?",
            back = "O(1) average. The assumption: a good hash function distributes keys uniformly " +
                "so bucket chains stay short. Worst case (all keys collide) is O(n), but this " +
                "is avoided by a quality hash function and a load factor kept below ~0.75.",
        )
        card(
            id = "hm-load-factor",
            front = "What is load factor, and what happens when it exceeds ~0.75?",
            back = "Load factor = (number of entries) / (number of buckets). Above ~0.75, " +
                "collision chains grow long enough to degrade lookup toward O(n). " +
                "Python's dict and Java's HashMap resize (rehash all entries) automatically " +
                "when the threshold is crossed, restoring O(1) amortized performance.",
        )
        card(
            id = "hm-vs-sorted",
            front = "When should you prefer a sorted array over a hash map?",
            back = "Sorted array when: you need range queries (all keys between A and B), " +
                "ordered iteration, or O(log n) guaranteed worst-case lookup (binary search). " +
                "Hash map when: you only need exact-key lookup and O(1) average beats O(log n). " +
                "Hash map has no ordering; sorted array has no O(1) insert.",
        )
        card(
            id = "hm-complement",
            front = "Why does storing complements in a hash map solve two-sum in O(n)?",
            back = "For each element x, the pair we need is (target - x). " +
                "By storing every seen value in a map keyed by value, we can check in O(1) " +
                "whether the complement exists. This turns 'search for the other element' " +
                "from O(n) per step into O(1) per step — one pass total.",
        )
    }
}
