package com.cortex.app.data.local.content.lessons

import com.cortex.app.data.local.content.lesson
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track

val Traversals = lesson("traversals") {
    track = Track.ALGORITHMS
    tier = Tier.FOUNDATIONS
    title = "BFS & DFS"

    hook {
        problem = """
            A platform engineering team maintains a microservice dependency graph.
            When service A is changed, they need to know the shortest chain of imports
            that connects A to every downstream service — to rank impact by proximity.
            Depth-first search finds a path. It does not find the shortest one.
        """.trimIndent()
        stakes = """
            Impact radius analysis, network routing, package dependency resolution,
            and social-graph queries all rely on graph traversal. Choosing DFS when
            BFS is required produces a path, not the shortest path — a subtle bug
            that only surfaces on graphs with more than one route between nodes.
        """.trimIndent()
    }

    intuition {
        narration(
            "BFS (Breadth-First Search) uses a queue. It processes all nodes at distance 1 " +
                "before any node at distance 2, all at distance 2 before distance 3, and so on. " +
                "This level-by-level expansion guarantees that the first time BFS reaches a node, " +
                "it has found the shortest path to it (in an unweighted graph).",
            "DFS (Depth-First Search) uses a stack — either an explicit one or the call stack " +
                "via recursion. It goes as deep as possible along one branch before backtracking " +
                "to try other branches. DFS is not guaranteed to find the shortest path, but it " +
                "uses less memory on sparse deep graphs and naturally handles cycle detection " +
                "and topological sort.",
            "Both algorithms require a visited set when traversing graphs (not trees). " +
                "Without it, a cycle causes infinite traversal. Mark each node visited " +
                "as soon as it is added to the queue (BFS) or first encountered (DFS).",
        )
        visual = null
    }

    workedExample {
        step("Graph: {A:[B,C], B:[D], C:[D,E], D:[F], E:[F], F:[]}")
        step("Find shortest path from A to F using BFS.")
        step(
            """
            from collections import deque

            def bfs_path(graph, start, end):
                queue = deque([[start]])
                visited = {start}
                while queue:
                    path = queue.popleft()
                    node = path[-1]
                    if node == end:
                        return path
                    for neighbor in graph.get(node, []):
                        if neighbor not in visited:
                            visited.add(neighbor)
                            queue.append(path + [neighbor])
                return None
            """.trimIndent()
        )
        step("Step 1: queue=[['A']], visited={A}. Dequeue ['A']. Enqueue ['A','B'], ['A','C'].")
        step("Step 2: Dequeue ['A','B']. Enqueue ['A','B','D'].")
        step("Step 3: Dequeue ['A','C']. Enqueue ['A','C','E']. ('D' already visited.)")
        step("Step 4: Dequeue ['A','B','D']. Enqueue ['A','B','D','F']. F is end — return.")
        step("Shortest path: A → B → D → F (length 3). A → C → E → F also has length 3 but BFS returns the first found.")
        step("---")
        step("DFS reachability check: does a path exist from A to F?")
        step(
            """
            def dfs_reachable(graph, start, end, visited=None):
                if visited is None:
                    visited = set()
                if start == end:
                    return True
                visited.add(start)
                for neighbor in graph.get(start, []):
                    if neighbor not in visited:
                        if dfs_reachable(graph, neighbor, end, visited):
                            return True
                return False
            """.trimIndent()
        )
    }

    fadedPractice {
        problem(id = "tr-p1", scaffold = 2) {
            prompt = """
                Given a binary tree as a dict {node: [left, right]} with None for missing children,
                return a list of lists representing the level-order traversal.
                Tree: {1:[2,3], 2:[4,5], 3:[None,6], 4:[], 5:[], 6:[]}
                Expected: [[1],[2,3],[4,5,6]]
            """.trimIndent()
            hint("Use BFS. Start with a queue containing the root. At each level, process all nodes currently in the queue before moving to the next level.")
            hint("Track level size at the start of each iteration: size = len(queue). Dequeue exactly that many nodes to collect one level.")
            answer = """
                from collections import deque

                def level_order(tree, root):
                    if root is None:
                        return []
                    result, queue = [], deque([root])
                    while queue:
                        level, size = [], len(queue)
                        for _ in range(size):
                            node = queue.popleft()
                            level.append(node)
                            for child in tree.get(node, []):
                                if child is not None:
                                    queue.append(child)
                        result.append(level)
                    return result
                # level_order(tree, 1) -> [[1],[2,3],[4,5,6]]
            """.trimIndent()
        }
        problem(id = "tr-p2", scaffold = 1) {
            prompt = """
                Given an undirected graph as an adjacency list and two nodes,
                return True if a path exists between them, False otherwise.
                graph = {0:[1,2], 1:[0,3], 2:[0], 3:[1], 4:[5], 5:[4]}
                path_exists(graph, 0, 3) -> True
                path_exists(graph, 0, 4) -> False
            """.trimIndent()
            hint("Use DFS or BFS with a visited set to avoid revisiting nodes in cycles.")
            answer = """
                def path_exists(graph, src, dst):
                    visited = set()
                    stack = [src]
                    while stack:
                        node = stack.pop()
                        if node == dst:
                            return True
                        if node in visited:
                            continue
                        visited.add(node)
                        stack.extend(graph.get(node, []))
                    return False
            """.trimIndent()
        }
        problem(id = "tr-p3", scaffold = 0) {
            prompt = """
                Count the number of connected components in an undirected graph.
                graph = {0:[1], 1:[0], 2:[3], 3:[2], 4:[]}
                Expected: 3  (components: {0,1}, {2,3}, {4})
            """.trimIndent()
            answer = """
                def count_components(graph):
                    visited = set()
                    count = 0
                    def dfs(node):
                        visited.add(node)
                        for neighbor in graph.get(node, []):
                            if neighbor not in visited:
                                dfs(neighbor)
                    for node in graph:
                        if node not in visited:
                            dfs(node)
                            count += 1
                    return count
                # count_components({0:[1],1:[0],2:[3],3:[2],4:[]}) -> 3
            """.trimIndent()
        }
    }

    transferProblem {
        prompt = """
            A CI system models build steps as a DAG. Nodes are step names; edges
            point from a step to its dependents. When step X fails, the system
            must re-run X and every step that depends on it (directly or transitively).
            Given the DAG and the name of the failed step, return the complete set of
            steps that must be re-run. What traversal strategy identifies this set,
            and why is the visited set essential here?
        """.trimIndent()
        solution = """
            DFS (or BFS) from the failed step, following edges in the dependent direction.

            def steps_to_rerun(dag, failed):
                # dag[step] = list of steps that depend on step
                rerun = set()
                def dfs(node):
                    rerun.add(node)
                    for dependent in dag.get(node, []):
                        if dependent not in rerun:
                            dfs(dependent)
                dfs(failed)
                return rerun

            The visited set (rerun) is essential because the dependency graph may
            contain diamond patterns — step Z depends on both Y1 and Y2, both of
            which depend on X. Without the set, Z would be enqueued and processed
            twice, causing duplicate re-runs and potential race conditions in
            parallel execution.

            BFS works equally well for reachability; DFS is simpler to implement
            recursively and uses O(depth) stack space vs BFS's O(width) queue space.
        """.trimIndent()
    }

    reviewCards {
        card(
            id = "tr-bfs-shortest",
            front = "Why does BFS guarantee the shortest path in an unweighted graph, but DFS does not?",
            back = "BFS expands nodes in order of distance from the source. The first time it " +
                "reaches a node, it has arrived via the fewest edges possible — any longer path " +
                "would have been discovered later. DFS follows one branch to its end before " +
                "backtracking, so it may reach a node via a long detour before trying the short route.",
        )
        card(
            id = "tr-visited",
            front = "Why is a visited set necessary for graph traversal but not for tree traversal?",
            back = "Trees are acyclic by definition — there is exactly one path between any two nodes, " +
                "so a traversal cannot loop. Graphs may have cycles: without a visited set, " +
                "BFS/DFS re-enters a node it has already processed, causing an infinite loop " +
                "that exhausts the queue (BFS) or the call stack (DFS).",
        )
        card(
            id = "tr-space",
            front = "Space complexity of BFS vs DFS — and when does the difference matter?",
            back = "BFS: O(w) where w is the maximum width (nodes at the widest level). " +
                "DFS: O(d) where d is the maximum depth. " +
                "On a wide shallow graph (e.g., social network from a hub), BFS's queue " +
                "can hold millions of nodes simultaneously. " +
                "On a deep narrow graph (e.g., a linked list as a graph), DFS's stack hits " +
                "Python's recursion limit. Match the algorithm to the graph's shape.",
        )
        card(
            id = "tr-dfs-uses",
            front = "Name three problems where DFS is the natural choice over BFS.",
            back = "1. Topological sort — DFS post-order naturally produces reverse topological order. " +
                "2. Cycle detection — DFS tracks the current recursion path; a back edge to an " +
                "ancestor reveals a cycle. " +
                "3. Connected component labelling — DFS floods each component in one call; " +
                "BFS works too but DFS is simpler to write recursively.",
        )
    }
}
