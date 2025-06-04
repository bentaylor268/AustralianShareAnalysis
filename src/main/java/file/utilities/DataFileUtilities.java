package file.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class DataFileUtilities {

    public String readFile(String fileName) {
        StringBuilder builder = new StringBuilder();
        try {
            File myObj = new File(fileName);
            if (! myObj.exists()) {
                return null;
            }

            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                builder.append(myReader.nextLine());
            }
            myReader.close();
        } catch (FileNotFoundException e) {

            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return builder.toString();
    }

    public void writeFile(String fileName, String response) {
        try {
            File myObj = new File(fileName);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName() + " see " + fileName);
            } else {
                System.out.println("Write file: about to overwrite. see " + fileName);
            }

            FileWriter myWriter = new FileWriter(fileName);
            myWriter.write(response);
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


    }

}
