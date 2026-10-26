# DAA Mini Project 8 - Merge Sort vs Multithreaded Merge Sort (Java)

## Aim
Implement merge sort and multithreaded merge sort, compare the time required by both,
and analyze each algorithm for the best case and the worst case.

## Files
| File | Purpose |
|------|---------|
| MergeSortProject.java | Sequential + multithreaded merge sort, input generators, benchmark |

## How to run (JDK 11+)
    javac MergeSortProject.java
    java MergeSortProject                      # default sizes 10k .. 2M
    java MergeSortProject 100000 500000        # custom sizes
    java -Ddepth=3 MergeSortProject            # optional: force thread depth (2^3 = 8 threads)

Output is printed to the console and saved to results.csv (open in Excel to draw graphs).

## Algorithm
**Sequential:** split the array into two halves, sort each recursively, then merge.
T(n) = 2T(n/2) + O(n).

**Multithreaded:** the left half is sorted in a new Thread while the current thread sorts
the right half; both are joined and then merged. Thread creation is limited by depth
(about the number of CPU cores) and by a size cutoff (5000 elements) so that thread
overhead does not outweigh the benefit.

## Test inputs
- **Best case:** already sorted array (fewest comparisons, about (n log n) / 2)
- **Average case:** random permutation
- **Reverse sorted:** also a cheap case for merge sort
- **Worst case:** specially constructed array where every merge fully interleaves
  the two halves (maximum comparisons, about n log n - n + 1)

## Complexity analysis
| | Sequential | Multithreaded (p cores) |
|---|---|---|
| Best case time | O(n log n) | O(n log n) total work |
| Average case | O(n log n) | same |
| Worst case | O(n log n) | same |
| Space | O(n) | O(n) |

Merge sort is O(n log n) in every case; best and worst differ only by the constant factor
(number of comparisons, roughly 2x). Multithreading does not reduce total work, but
independent halves run in parallel, so wall-clock time drops. The last merge is still
sequential, so speedup is limited (Amdahl's law) and is less than p on p cores.

## Observations for your report
1. Comparisons on sorted input are about half of the worst case (see "Comparisons" column).
2. For small n the multithreaded version is no faster (thread overhead); for large n on a
   multi-core CPU it is faster.
3. Speedup stays below the core count because of the sequential top-level merge and memory bandwidth.
4. A JVM warm-up phase runs first so JIT compilation does not distort the timings.
