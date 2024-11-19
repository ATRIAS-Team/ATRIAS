package io.github.agentsoz.ees.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import com.google.firebase.internal.NonNull;
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

            // Set the latitude and longitude directly under the agent's node
            agentRef.child("latitude").setValueAsync(location.getX());
            agentRef.child("longitude").setValueAsync(location.getY());

            System.out.println("Location updated for Agent ID: " + agentID);
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
