package io.github.agentsoz.ees.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.internal.NonNull;

import java.util.List;

@FunctionalInterface
public interface ChildAddedHandler<T> {
    void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName, List<T> list);
}