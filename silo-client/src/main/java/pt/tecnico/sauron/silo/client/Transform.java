package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.Silo.*;

import java.util.List;

public class Transform {

    static VecTimestamp UpdateTimestampIntoVecTimestamp(UpdateTimestamp updateTimestamp) {
        return new VecTimestamp(UpdateTimestampIntoUpdateArray(updateTimestamp));
    }

    static Integer[] UpdateTimestampIntoUpdateArray(UpdateTimestamp updateTimestamp) {
        int numReplicas = updateTimestamp.getTimestampCount();
        Integer[] updateArray = new Integer[numReplicas];
        List<Integer> updateList = updateTimestamp.getTimestampList();

        for (int i = 0; i < numReplicas; i++) {
            if (i < updateList.size() && updateList.get(i) != null) { // if the instance exists and the value isn't null
                updateArray[i] = updateList.get(i);
            }
            else {
                updateArray[i] = 0;
            }
        }

        return updateArray;
    }
}
