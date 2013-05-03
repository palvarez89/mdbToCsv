package converter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;

public class Conversor {

	Database db;

	public boolean simpleOpen(File databaseFile) {
		Database database = null;

		try {
			// System.out.print("Abriendo bd\n");
			database = Database.open(databaseFile, true);
			db = database;

		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} catch (NoClassDefFoundError e) {
			System.err.println("Error: " + e.getMessage());
		}

		if (db != null) {
			return true;
		} else {
			return false;
		}
	}

	public void simpleClose() {
		try {
			db.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Error: " + e.getMessage());
		}
	}

	public static void main(String[] args) {

		Conversor converter = new Conversor();
		if (!converter.simpleOpen(new File(args[0]))) {
			System.out.print("ERROR opening database file (mdb)\n");
			return;
		}
		System.out.print("Database opened\n");

		File file = new File(args[0]);
		String nombre = file.getName();
		String nombreSinExt = nombre.substring(0, nombre.lastIndexOf("."));
		List<String> tablas = converter.getTables();

		for (String table : tablas) {
			System.out.print("Extracting table: " + table+"\n");
			converter.dataCopy(table, args[1], nombreSinExt);
		}

		converter.simpleClose();
		System.out.print("DONE\n");

	}

	public List<String> getTables() {
		List<String> tablas = new ArrayList<String>();

		try {
			for (String table : db.getTableNames()) {
				// output.append(table);
				// output.append("\n");
				tablas.add(table);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Error: " + e.getMessage());
		}

		return tablas;

	}

	private void dataCopy(String tableName, String outputDir, String outputName) {

		// Opening file to outuput
		BufferedWriter out = null;
		try {
			// Create file
			FileWriter fstream = new FileWriter(outputDir + "/" + outputName
					+ "_" + tableName + ".txt");
			out = new BufferedWriter(fstream);

			// Close the output stream

		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
			return;
		}

		// Escribimos cabeceras

		StringBuilder output = new StringBuilder();

		try {
			Table table = db.getTable(tableName);

			if (table != null) {
				// for (Column column : table.getColumns()) {
				List<Column> lista = table.getColumns();

				for (int cnx = 0; cnx < lista.size(); cnx++) {
					Column column = lista.get(cnx);

					output.append(column.getName());
					if (cnx < (lista.size() - 1))
						output.append(",");

				}
				output.append("\n");

				out.write(output.toString());
				// System.out.println(output.toString());
			}
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}

		//
		String[] queryRowFields = { "*" };

		// choosing this format because it's what BIRT likes.
		Vector<String[]> queryOutput = null;

		try {
			// get the table, and throw an exception if it's not found.
			Table table = db.getTable(tableName);

			if (table == null) {
				throw new NullPointerException("Table " + tableName
						+ " does not exist in database " + db.toString());
			}

			// implement the Kleene Star to return all columns in the table
			if (queryRowFields[0].equals("*") && queryRowFields.length == 1) {
				queryRowFields = new String[table.getColumnCount()];
				int i = 0;
				for (Column c : table.getColumns()) {
					queryRowFields[i] = c.getName();
					i++;
				}
			}

			// a cursor to iterate over the table
			// using multiple Cursors is not threadsafe...more on that later
			Cursor c = Cursor.createCursor(table);

			// Since we don't evaluate conditions in this query, the result set
			// will have as many rows as the table does.
			queryOutput = new Vector<String[]>(queryRowFields.length);

			// a map of column names => column data for each row
			HashMap rowData;

			// a temporary row
			String[] rowFields;

			// temp vars for the column name/column value assignments
			String data;
			String fieldName;

			// For each row in the table
			for (int i = 0; i < table.getRowCount(); i++) {
				rowData = new HashMap(c.getNextRow());

				rowFields = new String[queryRowFields.length];

				// For each column in the table we're interested in
				// ("Champion Name", "Employee ID")
				for (int j = 0; j < rowFields.length; j++) {
					fieldName = queryRowFields[j];

					try {
						String tipo = rowData.get(fieldName).getClass()
								.getName();
						// get the value at this cursor position

						Date fechafix = new Date();

						if (Date.class.isAssignableFrom(rowData.get(fieldName)
								.getClass())) {
							Format formatter = new SimpleDateFormat(
									"yyyy-MM-dd HH:mm:ss");
							data = formatter.format(fechafix);

						} else {

							data = rowData.get(fieldName).toString();
						}
					} catch (NullPointerException e) {
						// if it's null, just add the empty string
						data = "";
					}

					// at this point, you can evaluate any query conditions here
					// based on the column name (fieldName) and its value (data)
					// to control whether or not this row gets added to the
					// result set.

					rowFields[j] = data;
				}

				queryOutput.add(rowFields);
			}

			if (queryOutput != null) {
				// print the query
				StringBuilder sb = new StringBuilder();
				// for(String[] row : queryOutput)
				for (int inx = 0; inx < queryOutput.size(); inx++) {
					String[] row = (String[]) queryOutput.get(inx);
					// for (String col : row) {
					for (int jnx = 0; jnx < row.length; jnx++) {
						String col = row[jnx];
						sb.append(col);
						if (jnx < (row.length - 1))
							sb.append(",");

					}
					sb.append("\n");
				}
				out.write(sb.toString());
				// System.out.println(sb.toString());
			}
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}

		// closing output

		try {
			out.close();
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
}
