package com.cortex.app.data.local.content.lessons

import com.cortex.app.data.local.content.lesson
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track

val Sorting = lesson("sorting") {
    track = Track.ALGORITHMS
    tier = Tier.FOUNDATIONS
    title = "Sorting & Complexity"

    hook {
        problem = """
            A payment processor receives a batch of 100,000 transactions and must flag
            any two transactions with identical amounts — a fraud signal. The naive
            approach checks every pair: O(n²) = 10 billion comparisons for 100 K entries.
            The batch window is 200 ms. The nested loop takes 40 seconds.
        """.trimIndent()
        stakes = """
            Sorting is the most frequently overlooked preprocessing step in backend work.
            A single O(n log n) sort unlocks O(n) deduplication, O(n) two-pointer passes,
            and O(log n) binary search — all on the same sorted array.
            The sort pays for itself the moment you run more than one linear pass on the data.
        """.trimIndent()
    }

    intuition {
        narration(
            "Sorting imposes structure. Before sorting: equal elements could be anywhere, " +
                "smallest and largest are unknown, no element's position tells you anything. " +
                "After sorting: equal elements are adjacent, minimum is at index 0, maximum " +
                "at n−1, and binary search cuts the search space in half at each step.",
            "O(n log n) is the lower bound for comparison-based sorting. Any algorithm that " +
                "determines order only by comparing elements needs at least log₂(n!) comparisons — " +
                "which by Stirling's approximation is Θ(n log n). Merge sort and Timsort " +
                "(Python's sort) achieve this bound.",
            "Stability means equal elements retain their original relative order after sorting. " +
                "Stable sorts matter when you sort on a secondary key first, then a primary key: " +
                "ties on the primary key preserve the secondary order. Python's sort is stable. " +
                "In-place unstable sorts (quicksort) can break multi-key ordering.",
        )
        visual = null
    }

    workedExample {
        step("Problem 1: find if any duplicates exist in [3, 1, 4, 1, 5, 9, 2, 6, 5].")
        step(
            """
            nums = [3, 1, 4, 1, 5, 9, 2, 6, 5]
            nums.sort()                  # [1, 1, 2, 3, 4, 5, 5, 6, 9]  O(n log n)
            has_dup = any(nums[i] == nums[i+1] for i in range(len(nums)-1))
            # True — adjacent scan is O(n); duplicates are neighbours after sort.
            """.trimIndent()
        )
        step("Problem 2: find the closest pair of numbers in an unsorted array.")
        step(
            """
            def closest_pair(arr):
                arr = sorted(arr)        # O(n log n)
                best = float('inf')
                pair = (arr[0], arr[1])
                for i in range(len(arr) - 1):
                    diff = arr[i+1] - arr[i]
                    if diff < best:
                        best = diff
                        pair = (arr[i], arr[i+1])
                return pair
            # closest_pair([10, 3, 17, 5, 4]) -> (4, 5); adjacent after sort.
            """.trimIndent()
        )
        step("Without sort, closest pair requires O(n²) brute force. Sort-first: O(n log n + n) = O(n log n).")
        step("Problem 3: dedup a sorted list in one pass.")
        step(
            """
            def dedup_sorted(nums):
                if not nums:
                    return []
                unique = [nums[0]]
                for n in nums[1:]:
                    if n != unique[-1]:
                        unique.append(n)
                return unique
            # dedup_sorted([1, 1, 2, 3, 4, 5, 5, 6, 9]) -> [1, 2, 3, 4, 5, 6, 9]
            """.trimIndent()
        )
    }

    fadedPractice {
        problem(id = "sort-p1", scaffold = 2) {
            prompt = "Check whether any duplicates exist in [7, 3, 5, 7, 2, 9] using a sort-first approach."
            hint("Sort the array, then scan adjacent pairs. If arr[i] == arr[i+1] for any i, a duplicate exists.")
            hint("After sorting: [2, 3, 5, 7, 7, 9]. Compare index 0-1, 1-2, ... The pair at indices 3-4 matches.")
            answer = """
                def has_duplicate(nums):
                    nums = sorted(nums)
                    return any(nums[i] == nums[i+1] for i in range(len(nums) - 1))
                # has_duplicate([7,3,5,7,2,9]) -> True (7 appears twice)
            """.trimIndent()
        }
        problem(id = "sort-p2", scaffold = 1) {
            prompt = "Find the two numbers with the smallest absolute difference in [15, 3, 8, 1, 10, 6]."
            hint("Sort first. The closest pair must be adjacent in a sorted array — any non-adjacent pair has a larger or equal difference.")
            answer = """
                def closest_pair(arr):
                    arr = sorted(arr)      # [1, 3, 6, 8, 10, 15]
                    best = float('inf')
                    result = (arr[0], arr[1])
                    for i in range(len(arr) - 1):
                        diff = arr[i+1] - arr[i]
                        if diff < best:
                            best = diff
                            result = (arr[i], arr[i+1])
                    return result
                # closest_pair([15,3,8,1,10,6]) -> (7, 8) ... actually (7,8) no:
                # sorted: [1,3,6,8,10,15]; diffs: 2,3,2,2,5 -> first min=2 at (1,3)
                # result: (1, 3)
            """.trimIndent()
        }
        problem(id = "sort-p3", scaffold = 0) {
            prompt = """
                Count the number of pairs (i, j) with i < j such that abs(arr[i] - arr[j]) == k.
                arr = [1, 5, 3, 4, 2], k = 2. Expected: 3 (pairs: (1,3), (3,5), (2,4)).
            """.trimIndent()
            answer = """
                def count_pairs_with_diff(arr, k):
                    arr.sort()              # [1, 2, 3, 4, 5]
                    count, left, right = 0, 0, 1
                    while right < len(arr):
                        diff = arr[right] - arr[left]
                        if diff == k:
                            count += 1
                            left += 1
                            right += 1
                        elif diff < k:
                            right += 1
                        else:
                            left += 1
                        if left == right:
                            right += 1
                    return count
                # count_pairs_with_diff([1,5,3,4,2], 2) -> 3
            """.trimIndent()
        }
    }

    transferProblem {
        prompt = """
            An analytics pipeline receives user session events out of order (network jitter).
            Each event has a (user_id, timestamp, duration_seconds). After collecting a batch,
            the pipeline must merge overlapping or adjacent sessions for the same user into
            a single session span, then emit the merged spans.
            Describe the algorithm — including sort key, merge condition, and time complexity —
            and explain why sorting first is cheaper than a priority queue or interval tree here.
        """.trimIndent()
        solution = """
            Sort by (user_id, timestamp). O(n log n). Then a single linear merge pass. O(n).

            from itertools import groupby

            def merge_sessions(events):
                events.sort(key=lambda e: (e['user_id'], e['timestamp']))
                result = []
                for _, group in groupby(events, key=lambda e: e['user_id']):
                    spans = list(group)
                    merged_start = spans[0]['timestamp']
                    merged_end   = spans[0]['timestamp'] + spans[0]['duration_seconds']
                    for ev in spans[1:]:
                        start = ev['timestamp']
                        end   = start + ev['duration_seconds']
                        if start <= merged_end:          # overlapping or adjacent
                            merged_end = max(merged_end, end)
                        else:
                            result.append({'user_id': spans[0]['user_id'],
                                           'start': merged_start, 'end': merged_end})
                            merged_start, merged_end = start, end
                    result.append({'user_id': spans[0]['user_id'],
                                   'start': merged_start, 'end': merged_end})
                return result

            Why not a priority queue? A priority queue processes events in order but
            requires O(n log n) to build and O(log n) per pop — same asymptotic cost,
            more implementation complexity. A sort-then-scan is simpler and has the
            same worst-case cost. An interval tree adds O(log n) query overhead per
            interval and is warranted only for online (streaming, one-event-at-a-time)
            overlap queries — not for batch processing where full-batch sort is allowed.
        """.trimIndent()
    }

    reviewCards {
        card(
            id = "sort-lower-bound",
            front = "What is the comparison-sort lower bound, and what does it mean in practice?",
            back = "Ω(n log n). Any algorithm that determines the sorted order of n elements " +
                "using only pairwise comparisons must make at least Θ(n log n) comparisons " +
                "in the worst case. Merge sort and Timsort are optimal. You can beat this " +
                "bound only with non-comparison sorts (counting sort, radix sort) that exploit " +
                "key structure — but those require bounded integer keys.",
        )
        card(
            id = "sort-stability",
            front = "What does sort stability mean, and when does it matter?",
            back = "A sort is stable if elements with equal keys appear in the same relative " +
                "order in the output as in the input. It matters for multi-key sorting: sort " +
                "by secondary key first, then by primary key with a stable sort — ties on the " +
                "primary key preserve the secondary ordering. Python's sort (Timsort) is stable. " +
                "C's qsort is not guaranteed stable.",
        )
        card(
            id = "sort-first-tradeoff",
            front = "When is paying O(n log n) to sort worth it?",
            back = "When the sorted structure enables multiple cheaper operations on the same data: " +
                "adjacent dedup O(n), two-pointer sum O(n), binary search O(log n). " +
                "Break-even is roughly one additional O(n) pass that would otherwise cost O(n²). " +
                "Not worth it for a single unsorted lookup (use a hash map instead).",
        )
        card(
            id = "sort-timsort",
            front = "What algorithm does Python use for list.sort(), and what are its key properties?",
            back = "Timsort — a hybrid of merge sort and insertion sort. " +
                "O(n log n) worst case, O(n) best case on already-sorted or nearly-sorted data " +
                "(it detects natural runs). Stable. In-place (O(n) auxiliary space for merging). " +
                "Same algorithm is used in Java's Arrays.sort() for objects.",
        )
    }
}
