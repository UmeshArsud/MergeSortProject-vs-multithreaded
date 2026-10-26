import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

/**
 * DAA Mini Project 8: Merge Sort vs Multithreaded Merge Sort
 * Compares running time and number of comparisons for best, average and worst case.
 *
 * Compile : javac MergeSortProject.java
 * Run     : java MergeSortProject            (default sizes)
 *           java MergeSortProject 100000 500000   (custom sizes)
 */
public class MergeSortProject {

    // ---------------- Merge step (shared by both versions) ----------------
    // Merges a[l..mid) and a[mid..r) using tmp. Returns number of comparisons.
    static long merge(int[] a, int[] tmp, int l, int mid, int r) {
        long cmp = 0;
        int i = l, j = mid, k = l;
        while (i < mid && j < r) {
            cmp++;
            if (a[i] <= a[j]) tmp[k++] = a[i++];
            else              tmp[k++] = a[j++];
        }
        while (i < mid) tmp[k++] = a[i++];
        while (j < r)   tmp[k++] = a[j++];
        System.arraycopy(tmp, l, a, l, r - l);
        return cmp;
    }

    // ---------------- 1. Sequential merge sort: T(n) = 2T(n/2) + O(n) ----------------
    static long mergeSort(int[] a, int[] tmp, int l, int r) {
        if (r - l <= 1) return 0;
        int mid = l + (r - l + 1) / 2;
        long c = mergeSort(a, tmp, l, mid);
        c += mergeSort(a, tmp, mid, r);
        c += merge(a, tmp, l, mid, r);
        return c;
    }

    // ---------------- 2. Multithreaded merge sort ----------------
    // Left half is sorted in a new thread, right half by the current thread.
    // 'depth' limits thread creation (about 2^depth threads) to avoid overhead.
    static final int CUTOFF = 5000;

    static long parallelMergeSort(int[] a, int[] tmp, int l, int r, int depth) throws InterruptedException {
        if (r - l <= 1) return 0;
        if (depth <= 0 || r - l < CUTOFF) return mergeSort(a, tmp, l, r);

        int mid = l + (r - l + 1) / 2;
        long[] leftCmp = new long[1];

        Thread t = new Thread(() -> {
            try {
                leftCmp[0] = parallelMergeSort(a, tmp, l, mid, depth - 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();
        long rightCmp = parallelMergeSort(a, tmp, mid, r, depth - 1);
        t.join();

        return leftCmp[0] + rightCmp + merge(a, tmp, l, mid, r);
    }

    // ---------------- Input generators ----------------
    static int[] genBest(int n) {                    // already sorted
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = i;
        return v;
    }

    static int[] genReverse(int n) {                 // reverse sorted
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = n - i;
        return v;
    }

    static int[] genRandom(int n) {                  // random permutation
        int[] v = genBest(n);
        Random rnd = new Random(42);
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = v[i]; v[i] = v[j]; v[j] = t;
        }
        return v;
    }

    // Worst case: every merge must fully interleave the two halves (max comparisons).
    // Built by "un-merging" a sorted array: even positions go left, odd go right.
    static void buildWorst(int[] v) {
        if (v.length <= 1) return;
        int ln = (v.length + 1) / 2, rn = v.length / 2;
        int[] L = new int[ln], R = new int[rn];
        for (int i = 0; i < v.length; i++) {
            if (i % 2 == 0) L[i / 2] = v[i]; else R[i / 2] = v[i];
        }
        buildWorst(L);
        buildWorst(R);
        System.arraycopy(L, 0, v, 0, ln);
        System.arraycopy(R, 0, v, ln, rn);
    }

    static int[] genWorst(int n) {
        int[] v = genBest(n);
        buildWorst(v);
        return v;
    }

    // ---------------- Benchmark helper ----------------
    static class Result {
        double ms; long cmp; boolean ok;
        Result(double ms, long cmp, boolean ok) { this.ms = ms; this.cmp = cmp; this.ok = ok; }
    }

    static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    static Result run(int[] input, boolean parallel, int depth, int reps) throws InterruptedException {
        double best = Double.MAX_VALUE;
        long cmp = 0;
        boolean ok = true;
        for (int r = 0; r < reps; r++) {
            int[] a = Arrays.copyOf(input, input.length);
            int[] tmp = new int[a.length];
            long t0 = System.nanoTime();
            cmp = parallel ? parallelMergeSort(a, tmp, 0, a.length, depth)
                           : mergeSort(a, tmp, 0, a.length);
            long t1 = System.nanoTime();
            best = Math.min(best, (t1 - t0) / 1e6);
            ok &= isSorted(a);
        }
        return new Result(best, cmp, ok);
    }

    // ---------------- Main ----------------
    public static void main(String[] args) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();
        int depth = 0;
        while ((1 << depth) < cores) depth++;        // ~cores threads
        depth = Integer.getInteger("depth", depth);   // optional override: java -Ddepth=3 ...

        int[] sizes = {10000, 50000, 100000, 500000, 1000000, 2000000};
        if (args.length > 0) {
            sizes = new int[args.length];
            for (int i = 0; i < args.length; i++) sizes[i] = Integer.parseInt(args[i]);
        }

        System.out.println("CPU cores available: " + cores + "  (thread depth = " + depth + ")\n");

        String[] names = {"Best (sorted)", "Average (random)", "Reverse sorted", "Worst (interleaved)"};

        // JVM warm-up so JIT compilation does not distort the first measurements
        int[] warm = genRandom(200000);
        for (int i = 0; i < 5; i++) { run(warm, false, depth, 1); run(warm, true, depth, 1); }

        try (PrintWriter csv = new PrintWriter("results.csv")) {
            csv.println("case,n,seq_ms,par_ms,speedup,seq_cmp,par_cmp");

            for (int c = 0; c < names.length; c++) {
                System.out.println("=== " + names[c] + " ===");
                System.out.printf("%-10s %-14s %-16s %-10s %-16s %s%n",
                        "n", "Seq (ms)", "Parallel (ms)", "Speedup", "Comparisons", "Sorted?");

                for (int n : sizes) {
                    int[] in;
                    switch (c) {
                        case 0:  in = genBest(n);    break;
                        case 1:  in = genRandom(n);  break;
                        case 2:  in = genReverse(n); break;
                        default: in = genWorst(n);   break;
                    }
                    int reps = n <= 100000 ? 7 : 5;
                    Result s = run(in, false, depth, reps);
                    Result p = run(in, true, depth, reps);
                    double speedup = s.ms / p.ms;

                    System.out.printf("%-10d %-14.3f %-16.3f %-10.2f %-16d %s%n",
                            n, s.ms, p.ms, speedup, s.cmp, (s.ok && p.ok) ? "yes" : "NO!");
                    csv.printf("\"%s\",%d,%.3f,%.3f,%.3f,%d,%d%n",
                            names[c], n, s.ms, p.ms, speedup, s.cmp, p.cmp);
                }
                System.out.println();
            }
        }
        System.out.println("Results saved to results.csv (open in Excel to draw graphs).");
    }
}
