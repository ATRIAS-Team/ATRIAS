/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.agentsoz.ees.jadexextension.masterthesis.clustering;

import elki.clustering.kmedoids.PAM;
import elki.clustering.kmedoids.initialization.BUILD;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MedoidModel;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDRange;
import elki.database.relation.Relation;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.datasource.DatabaseConnection;
import elki.distance.minkowski.SquaredEuclideanDistance;

public class KMedoidsPAMTest {
  public static void main(String[] args) {
    int numRows = 1000;    // Number of rows
    int numCols = 2;    // Number of columns

    double[][] data = new double[numRows][numCols];

    // Populate the 2D array with random double values using Math.random()
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numCols; j++) {
        // Generate a random double between 0.0 (inclusive) and 1.0 (exclusive)
        data[i][j] = Math.random() * 100;
      //todo: Read Trip Location data and cluster them, the result should be plotted with elki
       //data[i][j] =
      }
    }
      DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(data);
      Database db = new StaticArrayDatabase(dbc, null);
      db.initialize();
      SquaredEuclideanDistance dist = SquaredEuclideanDistance.STATIC;
      // OD Matrix
      PAM<NumberVector> pam2 = new PAM<>(dist, 10, 10, new BUILD<>());

      // Relation containing the number vectors:
      Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
      DBIDRange ids = (DBIDRange) rel.getDBIDs();

      // Run the algorithm:
      Clustering<MedoidModel> c = pam2.autorun(db);

      for (Cluster<MedoidModel> cluster : c.getAllClusters()) {
        System.out.println(cluster);
      }
  }
}
