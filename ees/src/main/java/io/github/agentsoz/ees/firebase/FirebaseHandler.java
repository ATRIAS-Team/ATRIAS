package io.github.agentsoz.ees.firebase;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import com.google.firebase.internal.NonNull;
import io.github.agentsoz.ees.shared.Cells;
import io.github.agentsoz.util.Location;

import java.io.FileInputStream;
import java.util.List;

public class FirebaseHandler<A, T> {
    private A agent;
    private List<T> list;

    public FirebaseHandler(A agent, List<T> list){
        this.agent = agent;
        this.list = list;
    }

    public static void init(){
        try {
            // Load the service account credentials from the JSON file
            FileInputStream serviceAccount = new FileInputStream
                    ("Firebase-sdk-key.json");
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

            // Configure Firebase with the loaded credentials and database URL
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setDatabaseUrl
                            ("https://maps-a5b66-default-rtdb.europe-west1.firebasedatabase.app/")
                    .build();

            // Initialize Firebase with the configured options
            FirebaseApp.initializeApp(options);

            // Log the successful initialization
            System.out.println("Firebase initialized successfully.");
        } catch (Exception e) {
            // Log any errors that occur during initialization
            e.printStackTrace();
            System.err.println("Error initializing Firebase: " + e.getMessage()); // Log the error
        }
    }

    public ChildEventListener childAddedListener(String path, ChildAddedHandler<T> childAddedHandler){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        return databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                childAddedHandler.onChildAdded(dataSnapshot, previousChildName, list);
            }


            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static void removeChildEventListener(String path, ChildEventListener childEventListener){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.removeEventListener(childEventListener);
    }

    public static void updateAgentLocation(String agentID, Location location) {
        try {
            DatabaseReference agentRef = FirebaseDatabase.getInstance()
                    .getReference("agents")
                    .child(agentID);

            double[] latlng = Cells.locationToLatLng(location);
            double lat = latlng[0];
            double lng = latlng[1];

            // Set the latitude and longitude directly under the agent's node
            agentRef.child("latitude").setValueAsync(lat);
            agentRef.child("longitude").setValueAsync(lng);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error updating location in Firebase: " + e.getMessage());
        }
    }

    public static void sendTripProgress(String tripId, String progress) {
        DatabaseReference tripRef = FirebaseDatabase.getInstance().getReference().child("trips").child(tripId);

        // Update the 'status' subnode with the progress
        tripRef.child("status").setValue(progress, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    // Success
                    System.out.println("Trip progress updated successfully. Progress: " + progress);
                } else {
                    // Handle the error
                    System.err.println("Error updating trip progress: " + databaseError.getMessage());
                }
            }
        });
    }

    public static void assignAgentToTripRequest(String tripID, String agentID){
        DatabaseReference tripRequests = FirebaseDatabase.getInstance().getReference("tripRequests");
        tripRequests.child(tripID).child("assignedAgent").setValueAsync(agentID);
    }
}
