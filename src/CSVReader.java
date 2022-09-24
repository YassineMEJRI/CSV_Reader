import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code CSVReader} provides a stateful API for streaming individual CSV rows
 * as arrays of strings that have been read from a given CSV file.
 *
 * @author YOUR-NAME-HERE
 */
public class CSVReader {
    int record_number = 0;
    int line_number = 0;
    enum STATE{ START, INFIELD, IN_QUOTES, AFTER_QUOTE}
    private static final int LF = 0;
    private static final int CR = 1;
    private static final int DQUOTE = 2;
    private static final int COMMA = 3;
    private static final int TEXTDATA = 4;
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 5130490650040L;
    private final CharacterReader reader;

    public CSVReader(CharacterReader reader) {
        this.reader = reader;
    }

    /**
     * This method uses the class's {@code CharacterReader} to read in just enough
     * characters to process a single valid CSV row, represented as an array of
     * strings where each element of the array is a field of the row. If formatting
     * errors are encountered during reading, this method throws a
     * {@code CSVFormatException} that specifies the exact point at which the error
     * occurred.
     *
     * @return a single row of CSV represented as a string array, where each
     *         element of the array is a field of the row; or {@code null} when
     *         there are no more rows left to be read.
     * @throws IOException when the underlying reader encountered an error
     * @throws CSVFormatException when the CSV file is formatted incorrectly
     */
    public String[] readRow() throws IOException, CSVFormatException {
        line_number++;
        int column_number = 0;
        record_number++;
        int field_number = 1;
        STATE state = STATE.START; //Current state of the FSM
        int c;
        StringBuilder string = new StringBuilder();
        List<String> stringList = new ArrayList<>();

        int char_class;

        //while not end of file
        while ((c = reader.read()) != -1) {
            column_number++;
            char_class = characterClass(c); //type of current read character

            //depending on the state of the SFM
            switch (state){
                //if at the start of a field
                case START:
                    switch (char_class){
                        case CR:
                        case TEXTDATA: //reading ordinary field
                            string.append((char)c);
                            state = STATE.INFIELD;
                            break;
                        case COMMA: // empty field
                            stringList.add("");
                            field_number++;
                            //state stays at the start
                            break;
                        case DQUOTE: //inside double quotes
                            state = STATE.IN_QUOTES;
                            break;
                        case LF: // end of line
                            stringList.add("");
                            return stringList.toArray(new String[0]);
                    }
                    break;

                //if reading a field
                case INFIELD:
                    switch (char_class){
                        case TEXTDATA: //reading ordinary field
                        case CR:
                            string.append((char)c);
                            //state stays the same
                            break;
                        case COMMA: // end of field
                            stringList.add(string.toString());
                            string.setLength(0); //reset string builder
                            state = STATE.START;
                            field_number++;
                            break;
                        case DQUOTE: //double quotes inside normal field throw exception
                            throw new CSVFormatException("Double quotes inside normal fields not in double quotes", line_number, column_number, record_number, field_number);
                        case LF: // end of line
                            if (string.charAt(string.length()-1) == '\r')
                                string.deleteCharAt(string.length()-1);
                            stringList.add(string.toString());
                            return stringList.toArray(new String[0]);
                    }
                    break;

                case IN_QUOTES:
                    switch (char_class){
                        case TEXTDATA: //reading data
                        case COMMA: // empty field
                        case LF:
                        case CR:
                            string.append((char)c);
                            line_number++;
                            break;
                        case DQUOTE:
                            state = STATE.AFTER_QUOTE;
                            break;
                    }
                    break;

                case AFTER_QUOTE:
                    switch (char_class){
                        case DQUOTE: //escaping quotes
                            string.append((char)c);
                            state = STATE.IN_QUOTES;
                            break;
                        case COMMA: //end of quotes
                            field_number++; column_number++;
                            stringList.add(string.toString());
                            string.setLength(0);
                            state = STATE.START;
                            break;
                        case LF:
                            stringList.add(string.toString());
                            return stringList.toArray(new String[0]);
                        case CR:
                            if (reader.read() == '\n'){
                                stringList.add(string.toString());
                                return stringList.toArray(new String[0]);
                            }
                            else
                                throw new CSVFormatException("Invalid double quotes closing", line_number, column_number, record_number, field_number);
                        default:
                            throw new CSVFormatException("Invalid double quotes closing", line_number, column_number, record_number, field_number);
                    }
            }
        }
        return null;
    }

    private int characterClass(int c) {
        switch (c) {
            case 10: return LF;
            case 13: return CR;
            case 34: return DQUOTE;
            case 44: return COMMA;
            default: return TEXTDATA;
        }
    }

    /**
     * Feel free to edit this method for your own testing purposes. As given, it
     * simply processes the CSV file supplied on the command line and prints each
     * resulting array of strings to standard out. Any reading or formatting errors
     * are printed to standard error.
     *
     * @param args command line arguments (1 expected)
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: CSVReader <filename.csv>");
            return;
        }

        /*
         * This block of code demonstrates basic usage of CSVReader's row-oriented API:
         * initialize the reader inside try-with-resources, initialize the CSVReader
         * using the reader, and repeatedly call next() until null is encountered. Since
         * CharacterReader implements AutoCloseable, the reader will be automatically
         * closed once the try block is exited.
         */
        var filename = args[0];
        try (var reader = new CharacterReader(filename)) {
            var csvReader = new CSVReader(reader);
            String[] row;
            while ((row = csvReader.readRow()) != null) {
                System.out.println(Arrays.toString(row));
            }
        } catch (IOException | CSVFormatException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
