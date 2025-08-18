package org.example;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Incidence matrix over (flow_id, var_id) using BitSets.
 * - Each flow row is a BitSet of var indices present for that flow.
 * - "AUTO" on a flow means it matches all known vars (the var-universe BitSet).
 */
public class AudienceIncidenceDemo {

    static class Incidence {
        // interners
        private final Map<String, Integer> flowIndex = new HashMap<>();
        private final Map<String, Integer> varIndex = new HashMap<>();
        private final List<String> reverseFlow = new ArrayList<>();
        private final List<String> reverseVar  = new ArrayList<>();

        // rows: per-flow bitmap of vars
        private final List<BitSet> rows = new ArrayList<>();
        // all known var indices
        private final BitSet universeVars = new BitSet();
        // flows wildcarded by AUTO
        private final BitSet autoFlow = new BitSet();

        private int iFlow(String flowId) {
            return flowIndex.computeIfAbsent(flowId, k -> {
                int i = reverseFlow.size();
                reverseFlow.add(k);
                rows.add(new BitSet());
                return i;
            });
        }

        private int iVar(String varId) {
            return varIndex.computeIfAbsent(varId, k -> {
                int j = reverseVar.size();
                reverseVar.add(k);
                universeVars.set(j);
                return j;
            });
        }

        /** Add a (flow_id, var_id) pair. var_id may be "AUTO". */
        public void addPair(String flowId, String varId) {
            int i = iFlow(flowId);
            if ("AUTO".equalsIgnoreCase(varId)) {
                autoFlow.set(i, true);
            } else {
                int j = iVar(varId);
                rows.get(i).set(j, true);
            }
        }

        /** Effective row = row ∪ (AUTO ? universe : ∅). */
        private BitSet effRow(int i) {
            BitSet r = (BitSet) rows.get(i).clone();
            if (autoFlow.get(i)) r.or(universeVars);
            return r;
        }

        /** DISTINCT var_id for flows in F. */
        public Set<String> distinctVarsForFlows(Collection<String> flows) {
            BitSet out = new BitSet();
            for (String f : flows) {
                Integer i = flowIndex.get(f);
                if (i != null) out.or(effRow(i));
            }
            return decodeVars(out);
        }

        /** DISTINCT flow_id for vars in V. Unknown vars are ignored. */
        public Set<String> distinctFlowsForVars(Collection<String> vars) {
            BitSet vMask = toVarMask(vars);
            Set<String> out = new LinkedHashSet<>();
            for (int i = 0; i < reverseFlow.size(); i++) {
                BitSet tmp = effRow(i);
                tmp.and(vMask);
                if (!tmp.isEmpty()) out.add(reverseFlow.get(i));
            }
            return out;
        }

        /** WHERE flow IN F AND var IN V → concrete (flow,var) pairs (strings). */
        public List<String[]> selectPairs(Collection<String> flows, Collection<String> vars) {
            BitSet vMask = toVarMask(vars);
            List<String[]> out = new ArrayList<>();
            for (String f : flows) {
                Integer i = flowIndex.get(f);
                if (i == null) continue;
                BitSet hit = effRow(i);
                hit.and(vMask);
                for (int j = hit.nextSetBit(0); j >= 0; j = hit.nextSetBit(j + 1)) {
                    out.add(new String[]{ f, reverseVar.get(j) });
                }
            }
            return out;
        }

        /** Point lookup: does (flow,var) hold? (AUTO counts as true). */
        public boolean cell(String flowId, String varId) {
            Integer i = flowIndex.get(flowId);
            Integer j = varIndex.get(varId);
            if (i == null || j == null) return false;
            return effRow(i).get(j);
        }

        /** Predicates on a single flow row vs a set of vars. */
        public boolean hasAny(String flowId, Collection<String> vars) {
            Integer i = flowIndex.get(flowId);
            if (i == null) return false;
            BitSet q = toVarMask(vars);
            BitSet tmp = effRow(i);
            tmp.and(q);
            return !tmp.isEmpty();
        }

        public boolean hasAll(String flowId, Collection<String> vars) {
            Integer i = flowIndex.get(flowId);
            if (i == null) return false;
            BitSet q = toVarMask(vars);
            BitSet row = effRow(i);
            BitSet missing = (BitSet) q.clone();
            missing.andNot(row);
            return missing.isEmpty();
        }

        public boolean hasNone(String flowId, Collection<String> vars) {
            Integer i = flowIndex.get(flowId);
            if (i == null) return true;
            BitSet q = toVarMask(vars);
            BitSet tmp = effRow(i);
            tmp.and(q);
            return tmp.isEmpty();
        }

        // ---------- helpers ----------
        private BitSet toVarMask(Collection<String> vars) {
            BitSet mask = new BitSet();
            for (String v : vars) {
                Integer j = varIndex.get(v);    // ignore unknown vars
                if (j != null) mask.set(j, true);
            }
            return mask;
        }

        private Set<String> decodeVars(BitSet bs) {
            Set<String> out = new LinkedHashSet<>();
            for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
                out.add(reverseVar.get(j));
            }
            return out;
        }

        public Set<String> allFlows() { return new LinkedHashSet<>(reverseFlow); }
        public Set<String> allVars()  { return new LinkedHashSet<>(reverseVar);  }
    }

    public static void main(String[] args) {
        Incidence inc = new Incidence();

        // ---- Your sample data ----
        // audienceSegments: [{flow_id:"100", var_id: AUTO}, {flow_id:"2000", var_id:"399"}]
        inc.addPair("100", "AUTO");
        inc.addPair("2000", "399");

        // Show what we have learned about the "universe" of vars
        System.out.println("All flows: " + inc.allFlows());
        System.out.println("All vars:  " + inc.allVars());
        System.out.println();

        // 1) SELECT DISTINCT flow_id WHERE var_id IN ('399')
        var flowsFor399 = inc.distinctFlowsForVars(List.of("399"));
        System.out.println("Flows having var 399: " + flowsFor399 + "   // expects [100, 2000]");
        // Because 100 is AUTO (matches all known vars), and 2000 explicitly has 399.

        // 2) SELECT DISTINCT var_id WHERE flow_id IN ('100')
        var varsFor100 = inc.distinctVarsForFlows(List.of("100"));
        System.out.println("Vars reachable from flow 100: " + varsFor100 + "   // expects [399]");
        // Only known var so far is 399, thus AUTO expands to that.

        // 3) WHERE flow_id IN (...) AND var_id IN (...) -> concrete pairs
        var pairs = inc.selectPairs(List.of("100", "2000"), List.of("399"));
        String pairsStr = pairs.stream().map(p -> "(" + p[0] + "," + p[1] + ")").collect(Collectors.joining(", "));
        System.out.println("Pairs for flows {100,2000} and var {399}: " + pairsStr + "   // expects (100,399), (2000,399)");

        // 4) Point lookup (cell)
        System.out.println("(100,399) exists? " + inc.cell("100", "399") + "   // true via AUTO");
        System.out.println("(2000,399) exists? " + inc.cell("2000", "399") + " // true explicit");
        System.out.println("(2000,777) exists? " + inc.cell("2000", "777") + " // false (unknown var)");
        System.out.println();

        // 5) Predicates for a single flow vs a set of vars
        System.out.println("Flow 100 hasAny {399}?  " + inc.hasAny("100", List.of("399")) + "  // true");
        System.out.println("Flow 100 hasAll {399}?  " + inc.hasAll("100", List.of("399")) + "  // true");
        System.out.println("Flow 100 hasNone {399}? " + inc.hasNone("100", List.of("399")) + " // false");

        System.out.println("Flow 2000 hasAny {399}?  " + inc.hasAny("2000", List.of("399")) + "  // true");
        System.out.println("Flow 2000 hasAll {399}?  " + inc.hasAll("2000", List.of("399")) + "  // true");
        System.out.println("Flow 2000 hasNone {399}? " + inc.hasNone("2000", List.of("399")) + " // false");
    }
}

interface Posting {
  int cardinality();
  boolean contains(int row);
  org.roaringbitmap.IntIterator iterator(); // can return a custom iterator for arrays/singletons
}

final class SinglePosting implements Posting {
  final int row;
  SinglePosting(int r){ row=r; }
  public int cardinality(){ return 1; }
  public boolean contains(int r){ return r==row; }
  public org.roaringbitmap.IntIterator iterator(){ 
    return new org.roaringbitmap.IntIterator() { boolean used=false; 
      public boolean hasNext(){ return !used; } 
      public int next(){ used=true; return row; } 
    };
  }
}

final class SmallPosting implements Posting {
  final it.unimi.dsi.fastutil.ints.IntArrayList rows;
  SmallPosting(it.unimi.dsi.fastutil.ints.IntArrayList r){ rows=r; }
  public int cardinality(){ return rows.size(); }
  public boolean contains(int r){ return rows.contains(r); } // or binary search if kept sorted
  public org.roaringbitmap.IntIterator iterator(){ 
    final var it = rows.iterator();
    return new org.roaringbitmap.IntIterator(){ 
      public boolean hasNext(){ return it.hasNext(); } 
      public int next(){ return it.nextInt(); } 
    };
  }
}

final class BitmapPosting implements Posting {
  final org.roaringbitmap.RoaringBitmap bm;
  BitmapPosting(org.roaringbitmap.RoaringBitmap b){ bm=b; }
  public int cardinality(){ return bm.getCardinality(); }
  public boolean contains(int r){ return bm.contains(r); }
  public org.roaringbitmap.IntIterator iterator(){ return bm.getIntIterator(); }
}

public class EqIndex {
  private final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<Posting> post = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();

  public static EqIndex build(int[] codes){
    // first pass: counts
    it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap cnt = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();
    for (int code : codes) cnt.addTo(code, 1);

    // second pass: allocate tiered structures
    java.util.Map<Integer, Object> tmp = new java.util.HashMap<>();
    EqIndex idx = new EqIndex();
    for (int r=0; r<codes.length; r++){
      int c = codes[r];
      int f = cnt.get(c);
      Object bucket = tmp.get(c);
      if (bucket == null){
        if (f == 1){
          idx.post.put(c, new SinglePosting(r));
        } else if (f <= 32){
          var list = new it.unimi.dsi.fastutil.ints.IntArrayList(f);
          list.add(r);
          tmp.put(c, list);
        } else {
          var bm = new org.roaringbitmap.RoaringBitmap();
          bm.add(r);
          tmp.put(c, bm);
        }
      } else if (bucket instanceof it.unimi.dsi.fastutil.ints.IntArrayList list){
        list.add(r);
        if (list.size() == f) { // finalize
          idx.post.put(c, new SmallPosting(list));
          tmp.remove(c);
        }
      } else if (bucket instanceof org.roaringbitmap.RoaringBitmap bm){
        bm.add(r);
        if (bm.getCardinality() == f) {
          idx.post.put(c, new BitmapPosting(bm));
          tmp.remove(c);
        }
      } // SinglePosting was already finalized
    }
    return idx;
  }

  public Posting posting(int code){ return post.get(code); }
  public int count(int code){ var p = post.get(code); return p==null ? 0 : p.cardinality(); }
}


