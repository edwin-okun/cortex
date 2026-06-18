package com.cortex.app.data.local.content.lessons

import com.cortex.app.data.local.content.lesson
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track

val Recursion = lesson("recursion") {
    track = Track.ALGORITHMS
    tier = Tier.FOUNDATIONS
    title = "Recursion & Backtracking"

    hook {
        problem = """
            A configuration loader must parse arbitrarily nested JSON: an object whose
            values may themselves be objects, lists, or scalars — depth unknown at compile time.
            An iterative solution requires you to manually manage a stack of (node, key, depth)
            tuples. That stack is exactly what the call stack already does.
        """.trimIndent()
        stakes = """
            File-system traversal, expression parsing, dependency resolution, and tree
            algorithms all have variable depth. Recursive code maps directly to the
            problem's recursive structure. Iterative rewrites are longer, error-prone,
            and offer no asymptotic advantage when the call stack is the right tool.
        """.trimIndent()
    }

    intuition {
        narration(
            "A recursive function solves a problem by reducing it to one or more smaller " +
                "instances of the same problem, plus a base case that stops the chain. " +
                "Each call gets its own stack frame — local variables, parameters, return address. " +
                "The call stack is the bookkeeping you would otherwise write by hand.",
            "Backtracking extends recursion with an undo step: try a choice, recurse into it, " +
                "then undo the choice before trying the next one. The recursion tree explores " +
                "all candidates; the undo step ensures each branch starts from a clean state.",
            "Space cost: the call stack grows one frame per level of recursion. " +
                "For depth d, that is O(d) space. Python's default recursion limit is 1000 frames; " +
                "deep recursion on large inputs requires an explicit stack or tail-call conversion.",
        )
        visual = null
    }

    workedExample {
        step("Problem: generate all subsets (power set) of [1, 2, 3].")
        step(
            """
            def subsets(nums):
                result = []
                def backtrack(start, current):
                    result.append(list(current))   # snapshot current subset
                    for i in range(start, len(nums)):
                        current.append(nums[i])    # choose
                        backtrack(i + 1, current)  # recurse
                        current.pop()              # undo
                backtrack(0, [])
                return result
            """.trimIndent()
        )
        step("Call tree for [1, 2, 3]:")
        step("backtrack(0, []) → append [] → try 1")
        step("  backtrack(1, [1]) → append [1] → try 2")
        step("    backtrack(2, [1,2]) → append [1,2] → try 3")
        step("      backtrack(3, [1,2,3]) → append [1,2,3] → loop empty, return")
        step("    pop 3 → current=[1,2]. Loop ends. pop 2 → current=[1].")
        step("  try 3: backtrack(3,[1,3]) → append [1,3] → return. pop 3.")
        step("pop 1 → current=[]. Try 2, 3 similarly.")
        step("Result: [[], [1], [1,2], [1,2,3], [1,3], [2], [2,3], [3]] — 8 subsets = 2^3.")
    }

    fadedPractice {
        problem(id = "rec-p1", scaffold = 2) {
            prompt = "Write a recursive function that returns the sum of a list of integers."
            hint("Base case: index equals len(nums), return 0.")
            hint("Recursive case: return nums[i] + sum_list(nums, i + 1). Pass an index, not a slice, to stay O(n).")
            answer = """
                def sum_list(nums, i=0):
                    if i == len(nums):
                        return 0
                    return nums[i] + sum_list(nums, i + 1)
                # sum_list([1, 2, 3, 4]) -> 10
            """.trimIndent()
        }
        problem(id = "rec-p2", scaffold = 1) {
            prompt = "Write a recursive function that checks whether a string is a palindrome."
            hint("Use left and right indices instead of slicing. Base case: left >= right. Compare s[left] and s[right], then recurse with left+1, right-1.")
            answer = """
                def is_palindrome(s, left=0, right=None):
                    if right is None:
                        right = len(s) - 1
                    if left >= right:
                        return True
                    if s[left] != s[right]:
                        return False
                    return is_palindrome(s, left + 1, right - 1)
                # is_palindrome("racecar") -> True
                # is_palindrome("hello")   -> False
            """.trimIndent()
        }
        problem(id = "rec-p3", scaffold = 0) {
            prompt = "Generate all permutations of a list of distinct integers using backtracking."
            answer = """
                def permutations(nums):
                    result = []
                    def backtrack(current, remaining):
                        if not remaining:
                            result.append(list(current))
                            return
                        for i in range(len(remaining)):
                            current.append(remaining[i])
                            backtrack(current, remaining[:i] + remaining[i+1:])
                            current.pop()
                    backtrack([], nums)
                    return result
                # permutations([1, 2, 3]) -> 6 permutations
            """.trimIndent()
        }
    }

    transferProblem {
        prompt = """
            A deployment orchestrator must start N microservices. Some services have
            no dependencies; some depend on others. The dependency graph is a DAG.
            The orchestrator needs to enumerate all valid start orderings (topological
            sorts) and return the first one where each service passes its health check
            within a given timeout, without starting a service before its dependencies.
            What algorithm structure solves this, and what is the key operation
            that makes it correct?
        """.trimIndent()
        solution = """
            Backtracking over topological orderings.

            At each step, collect all services whose dependencies are already started
            (in-degree zero in the remaining subgraph). Try each as the next service:
            start it, mark it done, recurse, then undo (unmark) before trying the next.

            def find_order(graph, started, result):
                ready = [s for s in graph if s not in started
                         and all(dep in started for dep in graph[s].deps)]
                if not ready:
                    return result if len(result) == len(graph) else None
                for service in ready:
                    started.add(service)
                    result.append(service)
                    if health_check(service):
                        found = find_order(graph, started, result)
                        if found:
                            return found
                    result.pop()       # undo
                    started.remove(service)
                return None

            The undo step is the key: it restores the exact state before the failed
            branch so the next candidate starts from a clean slate.
        """.trimIndent()
    }

    reviewCards {
        card(
            id = "rec-base-case",
            front = "What is the role of the base case in a recursive function?",
            back = "The base case is the condition under which the function returns without " +
                "making another recursive call. It provides the concrete answer for the " +
                "smallest subproblem and terminates the chain. Without a reachable base case, " +
                "the call stack grows until a StackOverflowError (Python: RecursionError).",
        )
        card(
            id = "rec-space",
            front = "What is the space complexity of a depth-d recursion, and why?",
            back = "O(d) space. Each recursive call adds one frame to the call stack containing " +
                "local variables, parameters, and the return address. The stack reaches maximum " +
                "depth d at the deepest branch. Python's default limit is 1000 frames; " +
                "deeper recursion requires sys.setrecursionlimit() or an iterative rewrite.",
        )
        card(
            id = "rec-backtrack-undo",
            front = "Why must backtracking include an undo step after each recursive call?",
            back = "The undo step restores the shared mutable state (e.g., the current list) " +
                "to what it was before the choice was made. Without it, the next sibling branch " +
                "starts from a corrupted state that includes choices from prior branches. " +
                "The undo guarantees each branch explores an independent path.",
        )
        card(
            id = "rec-tco",
            front = "Does Python optimise tail-recursive calls?",
            back = "No. Python does not perform tail-call optimisation (TCO). A tail-recursive " +
                "function in Python still consumes one stack frame per call. For large inputs, " +
                "convert to an iterative loop with an explicit stack, or increase the recursion " +
                "limit with sys.setrecursionlimit() (with caution — it doesn't prevent memory exhaustion).",
        )
    }
}
