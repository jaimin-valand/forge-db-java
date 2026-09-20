package com.jaimin.db.index;

import java.util.*;

/**
 * In-memory B+ tree used by ForgeDB's secondary indexes. Leaf nodes are linked
 * for ordered traversal; internal nodes route lookups using separator keys.
 */
public final class BPlusTree<K extends Comparable<K>, V> {
    private static final int MAX_KEYS = 15;
    private Node<K,V> root = new Leaf<>();

    public void put(K key, V value) {
        Objects.requireNonNull(key); Objects.requireNonNull(value);
        Split<K,V> split = insert(root, key, value);
        if (split != null) {
            Internal<K,V> parent = new Internal<>();
            parent.children.add(root); parent.children.add(split.right);
            parent.keys.add(split.separator); root = parent;
        }
    }

    public List<V> get(K key) {
        Leaf<K,V> leaf = leafFor(key);
        List<V> values = leaf.values.get(key);
        return values == null ? List.of() : List.copyOf(values);
    }

    public boolean contains(K key) { return !get(key).isEmpty(); }

    public List<Map.Entry<K,V>> scan() {
        List<Map.Entry<K,V>> out = new ArrayList<>();
        Leaf<K,V> leaf = firstLeaf();
        while (leaf != null) {
            for (var e : leaf.values.entrySet()) for (V v : e.getValue()) out.add(Map.entry(e.getKey(), v));
            leaf = leaf.next;
        }
        return out;
    }

    public int size() { int n=0; for (var ignored: scan()) n++; return n; }

    private Split<K,V> insert(Node<K,V> node, K key, V value) {
        if (node instanceof Leaf<K,V> leaf) {
            leaf.values.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            if (leaf.values.size() <= MAX_KEYS) return null;
            Leaf<K,V> right = new Leaf<>();
            int mid = leaf.values.size()/2; List<K> keys = new ArrayList<>(leaf.values.keySet());
            for (int i=mid;i<keys.size();i++) { K k=keys.get(i); right.values.put(k, leaf.values.remove(k)); }
            right.next = leaf.next; leaf.next = right;
            return new Split<>(right.values.firstKey(), right);
        }
        Internal<K,V> in=(Internal<K,V>)node; int childIndex=upperBound(in.keys,key); Node<K,V> child=in.children.get(childIndex);
        Split<K,V> split=insert(child,key,value); if(split==null)return null;
        in.keys.add(childIndex, split.separator); in.children.add(childIndex+1, split.right);
        if(in.keys.size()<=MAX_KEYS)return null;
        int mid=in.keys.size()/2; K separator=in.keys.get(mid); Internal<K,V> right=new Internal<>();
        right.keys.addAll(in.keys.subList(mid+1,in.keys.size())); right.children.addAll(in.children.subList(mid+1,in.children.size()));
        in.keys.subList(mid,in.keys.size()).clear(); in.children.subList(mid+1,in.children.size()).clear();
        return new Split<>(separator,right);
    }

    private Leaf<K,V> leafFor(K key) { Node<K,V> n=root; while(n instanceof Internal<K,V> in)n=in.children.get(upperBound(in.keys,key)); return (Leaf<K,V>)n; }
    private Leaf<K,V> firstLeaf(){Node<K,V> n=root;while(n instanceof Internal<K,V> in)n=in.children.getFirst();return (Leaf<K,V>)n;}
    private static <K extends Comparable<K>> int upperBound(List<K> a,K k){int lo=0,hi=a.size();while(lo<hi){int m=(lo+hi)>>>1;if(a.get(m).compareTo(k)<=0)lo=m+1;else hi=m;}return lo;}
    private sealed interface Node<K,V> permits Leaf,Internal {}
    private static final class Leaf<K,V> implements Node<K,V>{final NavigableMap<K,List<V>> values=new TreeMap<>();Leaf<K,V> next;}
    private static final class Internal<K,V> implements Node<K,V>{final List<K> keys=new ArrayList<>();final List<Node<K,V>> children=new ArrayList<>();}
    private record Split<K,V>(K separator, Node<K,V> right){}
}
