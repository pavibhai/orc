/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.orc.filter.impl;

import org.apache.hadoop.hive.ql.exec.vector.ColumnVector;
import org.apache.orc.filter.OrcFilterContext;
import org.apache.orc.filter.VectorFilter;

public class IsNotNullFilter implements VectorFilter {

  private final String colName;

  public IsNotNullFilter(String colName) {
    this.colName = colName;
  }

  @Override
  public void filter(OrcFilterContext fc,
                     Selected bound,
                     Selected selIn,
                     Selected selOut) {
    ColumnVector v = fc.findColumnVector(colName);
    int inIdx = 0;
    int currSize = 0;
    int rowIdx;

    if (v.noNulls) {
      // In case we don't have any nulls, then irrespective of the repeating status, select all the
      // values
      for (int i = 0; i < bound.selSize; i++) {
        rowIdx = bound.sel[i];

        if (inIdx < selIn.selSize && rowIdx == selIn.sel[inIdx]) {
          // Row already protected, no need to evaluate
          inIdx++;
          continue;
        }

        // Select the row
        selOut.sel[currSize++] = rowIdx;
      }
    } else if (!v.isRepeating) {
      // As we have at least one null in this branch, we are only need to check if it is repeating
      // otherwise the repeating value will be null.
      for (int i = 0; i < bound.selSize; i++) {
        rowIdx = bound.sel[i];

        if (inIdx < selIn.selSize && rowIdx == selIn.sel[inIdx]) {
          // Row already protected, no need to evaluate
          inIdx++;
          continue;
        }

        // Select if the value is not null
        if (!v.isNull[rowIdx]) {
          selOut.sel[currSize++] = rowIdx;
        }
      }
    }

    selOut.selSize = currSize;
  }

}
