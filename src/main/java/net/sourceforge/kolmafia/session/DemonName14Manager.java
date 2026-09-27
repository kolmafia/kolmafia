package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Demon Name Solver - finds all valid demon names from syllable segments that satisfy specific
 * constraints (exactly 9 syllables, using all segment clues).
 */
public class DemonName14Manager {

  // List of all possible syllables
  private static final Set<String> SYLLABLES =
      Set.of(
          "Arg", "Bal", "Bar", "Bob", "But", "Cak", "Cal", "Call", "Car", "Col", "Cor", "Cul",
          "Cur", "Cut", "Dak", "Dar", "Dor", "Gar", "Ger", "Gra", "Gur", "Har", "Hur", "Hut", "Kar",
          "Kil", "Kir", "Kru", "Kul", "Kur", "Lag", "Lar", "Mor", "Nar", "Nix", "Nut", "Pha", "Rog",
          "Yer");

  /**
   * This core directed graph data structure represents the demon name segments as potential
   * mappings from known syllables to known syllables
   */
  private static class Graph {
    /**
     * Node in the graph. This represents a syllable and contains a set of segments that could
     * possibly reference it. e.g. the segment "rgB" would produce the nodes "Arg" and "Bal"
     *
     * @param segments Segments that reference ONLY this syllable (i.e. not part of an edge)
     */
    private record GraphNode(String syllable, Set<String> segments) {
      private GraphNode(String syllable, Set<String> segments) {
        this.syllable = syllable;
        this.segments = new HashSet<>(segments);
      }
    }

    /**
     * Edge in the graph. This represents one syllable leading to another (and is thus directed).
     * e.g. the segment "rgB" would produce an edge from "Arg" to "Bal"
     *
     * @param segments segments that create this edge
     */
    private record GraphEdge(String from, String to, Set<String> segments) {
      private GraphEdge(String from, String to, Set<String> segments) {
        this.from = from;
        this.to = to;
        this.segments = new HashSet<>(segments);
      }
    }

    /** Creates a mini directed graph for a segment showing all possible syllable transitions */
    private static Graph createFromSegment(String segment) {
      var graph = new Graph();
      graph.addSegment(segment);

      // Check if segment represents a single syllable
      for (String syllable : SYLLABLES) {
        // Use contains because some syllables are 4-characters long
        if (syllable.contains(segment)) {
          graph.addNode(syllable, segment);
        }
      }

      // Check all possible syllable-to-syllable transitions
      for (String from : SYLLABLES) {
        for (String to : SYLLABLES) {
          // Check if we can form the segment by taking suffix of `from` + prefix of `to`
          for (int splitPos = 1; splitPos < 3; splitPos++) {
            if (splitPos >= segment.length()) continue;

            String fromPart = segment.substring(0, splitPos);
            String toPart = segment.substring(splitPos);

            // Check if `from` ends with `fromPart` and `to` starts with `toPart`
            if (from.endsWith(fromPart) && to.startsWith(toPart)) {
              graph.addNode(from);
              graph.addNode(to);
              graph.addEdge(from, to, segment);
            }
          }
        }
      }

      return graph;
    }

    /**
     * Creates and composes individual segment graphs into one unified directed graph with metadata
     * tracking which segments contributed to each node/edge
     */
    private static Graph createFromSegments(Collection<String> segments) {
      var graph = new Graph();

      // Process each segment graph
      for (var segment : segments) {
        var segmentGraph = Graph.createFromSegment(segment);
        // For each segment in the graph (should be a single-element list)
        graph.addSegment(segment);
        // Add all edges from the segment graph
        for (GraphEdge edge : segmentGraph.getEdges()) {
          graph.addEdge(edge.from, edge.to, segment);
        }
        for (GraphNode node : segmentGraph.getNodes()) {
          graph.addNode(node.syllable, node.segments);
        }
      }

      return graph;
    }

    Map<String, GraphNode> nodeMap = new HashMap<>();
    Map<String, GraphEdge> edgeMap = new HashMap<>();
    Set<String> segments = new HashSet<>();

    public Collection<GraphNode> getNodes() {
      return this.nodeMap.values();
    }

    public Collection<GraphEdge> getEdges() {
      return this.edgeMap.values();
    }

    public void addNode(final String syllable) {
      this.addNode(syllable, (Set<String>) null);
    }

    public void addNode(final String syllable, final Set<String> segments) {
      this.nodeMap.compute(
          syllable,
          (key, node) -> {
            if (node == null) node = new GraphNode(syllable, new HashSet<>());
            if (segments != null) node.segments.addAll(segments);
            return node;
          });
    }

    public void addNode(final String syllable, final String segment) {
      this.addNode(syllable, Set.of(segment));
    }

    public void addEdge(final String from, final String to, final String segment) {
      this.edgeMap.compute(
          from + "->" + to,
          (key, edge) -> {
            if (edge == null) edge = new GraphEdge(from, to, new HashSet<>());
            edge.segments.add(segment);
            return edge;
          });
    }

    public void addSegment(final String segment) {
      this.segments.add(segment);
    }
  }

  /** Path during solving */
  private record SolverPath(List<String> syllables, Set<String> usedSegments) {
    private SolverPath(List<String> syllables, Set<String> usedSegments) {
      this.syllables = new ArrayList<>(syllables);
      this.usedSegments = new HashSet<>(usedSegments);
    }
  }

  /** Result of solving */
  private record SolverResult(String demonName, List<String> path, Set<String> usedSegments) {
    private SolverResult(String demonName, List<String> path, Set<String> usedSegments) {
      this.demonName = demonName;
      this.path = new ArrayList<>(path);
      this.usedSegments = new HashSet<>(usedSegments);
    }

    @Override
    public int hashCode() {
      return this.demonName.hashCode();
    }
  }

  /**
   * Finds all paths in a composed directed graph that satisfy the demon name constraints: - Exactly
   * 9 nodes (syllables) are visited - Edges from every subgraph (segment) are used at least once
   */
  private static Set<String> solveGraph(Graph graph) {
    Set<SolverResult> results = new HashSet<>();
    Set<String> allSegments = new HashSet<>(graph.segments);

    // Try starting from each node
    for (var node : graph.getNodes()) {
      String startSyllable = node.syllable;
      SolverPath initialPath = new SolverPath(List.of(startSyllable), new HashSet<>());

      dfs(graph, startSyllable, initialPath, allSegments, results);
    }

    return results.stream().map(r -> r.demonName).collect(Collectors.toSet());
  }

  /** Depth-first search to find valid paths */
  private static void dfs(
      Graph graph,
      String currentSyllable,
      SolverPath currentPath,
      Set<String> requiredSegments,
      Set<SolverResult> results) {
    // Base case: if we've visited exactly 9 syllables
    if (currentPath.syllables.size() == 9) {
      // Check if we've used edges from all segments. If a segment happens to be a node, count
      // visiting it as using it
      Set<String> allUsed = new HashSet<>(currentPath.usedSegments);
      allUsed.addAll(currentPath.syllables);

      if (!hasAllRequiredSegments(allUsed, requiredSegments)) {
        return;
      }

      String demonName = String.join("", currentPath.syllables);
      results.add(new SolverResult(demonName, currentPath.syllables, currentPath.usedSegments));
      return;
    }

    // Pruning: if we've already visited 9 or more syllables, stop
    if (currentPath.syllables.size() >= 9) return;

    // Find current node
    var node =
        graph.getNodes().stream()
            .filter(n -> n.syllable.equals(currentSyllable))
            .findFirst()
            .orElse(null);

    // Find all outgoing edges from current syllable
    var outgoingEdges =
        graph.getEdges().stream().filter(edge -> edge.from.equals(currentSyllable)).toList();

    for (var edge : outgoingEdges) {
      String nextSyllable = edge.to;

      List<String> newSyllables = new ArrayList<>(currentPath.syllables);
      newSyllables.add(nextSyllable);

      Set<String> newUsedSegments = new HashSet<>(currentPath.usedSegments);
      newUsedSegments.addAll(edge.segments);
      if (node != null) newUsedSegments.addAll(node.segments);

      SolverPath newPath = new SolverPath(newSyllables, newUsedSegments);

      // Continue DFS
      dfs(graph, nextSyllable, newPath, requiredSegments, results);
    }
  }

  /** Check if all required segment indices are present in the used set */
  private static boolean hasAllRequiredSegments(
      Set<String> usedSegments, Set<String> requiredSegments) {
    return usedSegments.containsAll(requiredSegments);
  }

  /** Find all valid demon names from segments */
  public static Set<String> solve(Collection<String> segments) {
    var graph = Graph.createFromSegments(segments);
    return solveGraph(graph);
  }
}
