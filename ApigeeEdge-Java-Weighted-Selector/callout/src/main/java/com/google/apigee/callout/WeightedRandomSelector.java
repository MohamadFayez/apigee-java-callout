// Copyright 2018 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.callout;

import java.util.ArrayList;

public class WeightedRandomSelector {

    // This implements a weighted random selector, over an array. The input argument a
    // must be an array of integers, weights for each bucket. The items need not be in
    // any particular order. The return value of the select() function is the bucket
    // number. Example usage:
    //
    // int[] weights = new int[] { 3, 50, 10, 20, 10, 47, 8};
    //
    // WeightedRandomSelector wrs = new WeightedRandomSelector(weights);
    // int bucket = wrs.select();
    //

    private int totalWeight= 0;
    private int L = 0;
    private int[] selectionCounts;
    private int[] weightThreshold;

    public WeightedRandomSelector(Iterable<Integer> a) {
        ArrayList<Integer> counts = new ArrayList<Integer>();
        ArrayList<Integer> thresholds = new ArrayList<Integer>();
        for (Integer v : a) {
            this.totalWeight += v;
            thresholds.add(this.totalWeight);;
            counts.add(0);
        }
        this.selectionCounts = counts.stream().mapToInt(i -> i).toArray();
        this.weightThreshold = thresholds.stream().mapToInt(i -> i).toArray();
        L = selectionCounts.length;
    }

    // private void dumpArray(String label, int[] a) {
    //     int i;
    //     for (i=0; i<a.length; i++) {
    //         System.out.printf("%s[%d]=(%d)\n", label, i, a[i]);
    //     }
    // }

    public int select() {
        // select a random value
        int R = (int) Math.floor(Math.random() * this.totalWeight), i;
        // now find the bucket that R value falls into.
        for (i = 0; i < L; i++) {
            if (R < this.weightThreshold[i]) {
                this.selectionCounts[i]++;
                return i;
            }
        }
        this.selectionCounts[L-1]++;
        return L - 1;
    }

}
