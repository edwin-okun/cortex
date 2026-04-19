package com.cortex.app.data.local.content.lessons

import com.cortex.app.data.local.content.lesson
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track
import com.cortex.app.domain.model.VisualSpec

val TwoPointers = lesson("two-pointers") {
    track = Track.ALGORITHMS
    tier = Tier.FOUNDATIONS
    title = "Two Pointers"

    hook {
        problem = """
            You have a sorted array and need to find two numbers that sum to a target.
            Brute force checks every pair — O(n²). For 10,000 elements that's 100 million checks.
        """.trimIndent()
        stakes = """
            Sorted arrays appear everywhere in backend systems: event logs, user ID ranges,
            leaderboards. The difference between O(n²) and O(n) is the difference between
            code that hangs production and code that runs in microseconds.
        """.trimIndent()
    }

    intuition {
        narration(
            "Imagine two people reading a book from opposite ends. When their fingers meet, " +
                "they've covered the whole book — and they got there faster than one person reading alone.",
            "Apply this to a sorted array: start left=0, right=n−1. " +
                "If arr[left] + arr[right] == target, done. " +
                "Sum too small? Move left right (increases sum). Sum too big? Move right left (decreases sum).",
            "Why can't we miss the pair? Because the array is sorted. Every pointer move is " +
                "monotone — left only increases, right only decreases. A missed pair would require " +
                "a pointer to move in the wrong direction, which never happens.",
        )
        visual = VisualSpec.ArrayPointerVisual(
            array = listOf(1, 3, 5, 7, 9),
            leftLabel = "left",
            rightLabel = "right",
        )
    }

    workedExample {
        step("arr = [1, 3, 5, 7, 9], target = 10")
        step("left=0 → arr[0]=1, right=4 → arr[4]=9. Sum = 1+9 = 10 == target. Done.")
        step("Pair found: (1, 9).")
        step("---")
        step("Harder: arr = [1, 3, 5, 7, 9], target = 12")
        step("left=0 (1), right=4 (9). Sum=10 < 12. Too small → move left.")
        step("left=1 (3), right=4 (9). Sum=12 == 12. Done. Pair: (3, 9).")
        step("Total pointer moves: 2. Brute force would take up to 10 comparisons.")
    }

    fadedPractice {
        problem(id = "tp-p1", scaffold = 2) {
            prompt = "Find the pair summing to 8 in arr = [1, 2, 4, 5, 7]."
            hint("Start: left=0 (value 1), right=4 (value 7). What is 1+7?")
            hint("Sum equals 8. You're done on the first check.")
            answer = "Pair: (1, 7). left=0, right=4. 1+7=8. Found immediately."
        }
        problem(id = "tp-p2", scaffold = 1) {
            prompt = "Find the pair summing to 9 in arr = [2, 3, 4, 6, 8]."
            hint("Start: left=0 (2), right=4 (8). Sum=10 > 9. Which pointer do you move?")
            answer = "Pair: (3, 6). left=0 (2)+right=4 (8)=10>9, move right. left=0 (2)+right=3 (6)=8<9, move left. left=1 (3)+right=3 (6)=9. Done."
        }
        problem(id = "tp-p3", scaffold = 0) {
            prompt = "Find the pair summing to 13 in arr = [1, 4, 5, 8, 9]."
            answer = "Pair: (4, 9). 1+9=10<13, move left. 4+9=13. Done."
        }
    }

    transferProblem {
        prompt = """
            A warehouse stores shipment weights sorted lightest-to-heaviest. Find the first
            and last shipment IDs whose combined weight exactly matches a truck's capacity.
            Weights: 1–100 kg. Array size: up to 50,000 entries.
            What algorithm solves this in the minimum complexity, and why?
        """.trimIndent()
        solution = """
            Two pointers: O(n) time, O(1) space.
            Start left at index 0 (lightest), right at n−1 (heaviest).
            If combined weight == capacity: return both IDs.
            If too light: move left right. If too heavy: move right left.
            Correctness relies on the array being sorted — pointer moves are monotone,
            so no valid pair can be skipped. Each pointer moves at most n steps total.
        """.trimIndent()
    }

    reviewCards {
        card(
            id = "tp-invariant",
            front = "What is the loop invariant for two-pointer sum search?",
            back = "If a valid pair exists, it lies within [left, right] at every iteration. " +
                "Proof: if arr[left]+arr[right] < target, arr[left] cannot pair with any index ≤ right to hit target " +
                "(all such values are ≤ arr[right]). So moving left right cannot skip a pair.",
        )
        card(
            id = "tp-complexity",
            front = "Time and space complexity of two-pointer sum search?",
            back = "O(n) time — each pointer travels at most n steps total, they never backtrack. " +
                "O(1) space — two index variables, no auxiliary structure.",
        )
        card(
            id = "tp-prereq",
            front = "What array property does two-pointer sum search require, and why?",
            back = "Sorted order. Sorting guarantees that moving left right strictly increases " +
                "the pair sum and moving right left strictly decreases it. Without sorting, " +
                "pointer moves have no guaranteed direction toward the target.",
        )
    }
}
