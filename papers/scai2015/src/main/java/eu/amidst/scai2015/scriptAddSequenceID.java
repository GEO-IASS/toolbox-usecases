/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.scai2015;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.filereaders.DataInstanceImpl;
import eu.amidst.core.datastream.filereaders.DataRow;
import eu.amidst.core.datastream.filereaders.DataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ana@cs.aau.dk on 22/05/15.
 */
public class scriptAddSequenceID {

    public static int numOfClients = 50000;

    public static void scriptAddSequenceID(String path)  throws IOException {
        ARFFDataReader reader= new ARFFDataReader();
        reader.loadFromFile(path);

        Attributes atts = reader.getAttributes();
        List<Attribute> newAttsList= new ArrayList<>();
        int cont=0;
        newAttsList.add(new Attribute (cont++, "SEQUENCE_ID", new RealStateSpace()));
        for(Attribute att: reader.getAttributes().getList()){
            newAttsList.add(new Attribute(cont++,att.getName(), att.getStateSpaceType()));
        }

        Attributes newAtts = new Attributes(newAttsList);
        String newPath = path.replace(".arff", "_SEQ_ID.arff");

        FileWriter fw = new FileWriter(newPath);
        fw.write("@relation dataset\n\n");

        for (Attribute att : newAtts){
            fw.write(ARFFDataWriter.attributeToARFFString(att)+"\n");
        }

        fw.write("\n\n@data\n\n");

        DataStreamFromFile data = new DataStreamFromFile(reader);

        Attribute timeID = atts.getAttributeByName("TIME_ID");

        //seqID and counter respectively
        int[] auxValues = new int[]{0,0};

        Attributes attributes = new Attributes(newAttsList);
        data.stream().forEach(e -> {

            if(auxValues[0]!=e.getValue(timeID)) {
                auxValues[0] = (int)e.getValue(timeID);
                auxValues[1] = 0;
            }

            DataRow dataRow = new DataRowFromAtts(attributes);
            for (Attribute att : atts) {
                dataRow.setValue(newAtts.getAttributeByName(att.getName()), e.getValue(att));
            }

            dataRow.setValue(newAtts.getAttributeByName("SEQUENCE_ID"), auxValues[1]);

            DataInstance assignment = new DataInstanceImpl(dataRow);
            try {
                fw.write(ARFFDataWriter.dataInstanceToARFFString(newAtts, assignment) + "\n");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            auxValues[1]++;

        });

        fw.close();

    }


    private static class DataRowFromAtts implements DataRow {
        private Map<Attribute,Double> assignment;
        private Attributes attributes;

        public DataRowFromAtts(Attributes attributes_){
            attributes = attributes_;
            assignment = new ConcurrentHashMap(attributes.getNumberOfAttributes());
        }
        @Override
        public double getValue(Attribute key){
            Double val = assignment.get(key);
            if (val!=null){
                return val.doubleValue();
            }
            else {
                //throw new IllegalArgumentException("No value stored for the requested variable: "+key.getName());
                return Utils.missingValue();
            }
        }
        @Override
        public void setValue(Attribute att, double val) {
            this.assignment.put(att,val);
        }

        @Override
        public Attributes getAttributes() {
            return this.attributes;
        }

        @Override
        public double[] toArray() {
            throw new UnsupportedOperationException("Operation not supported in Assigment object");
        }


        // Now you can use the following loop to iterate over all assignments:
        // for (Map.Entry<Variable, Double> entry : assignment.entrySet()) return entry;
        public Set<Map.Entry<Attribute,Double>> entrySet(){
            return assignment.entrySet();
        }

    }

    public static void main(String[] args) {
        try {
            //scriptAddSequenceID("/Users/ana/Documents/core/scai2015/datasets/dynamicDataOnlyContinuous.arff");
            scriptAddSequenceID(args[0]);
        }catch (IOException ex){}
    }
}
