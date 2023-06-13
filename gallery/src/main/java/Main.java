import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.w3c.dom.events.MouseEvent;

import com.google.api.core.ApiFuture;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;



public class Main {

    private static final String PROJECT_ID = "javaimagegallery-68cd9";
    private static final String BUCKET_NAME = "javaimagegallery-68cd9.appspot.com";
    private static final String SERVICE_ACCOUNT_KEY_PATH = "Gallery\\src\\main\\java\\serviceAccountKey.json";

    private JFrame frame;
    private JPanel contentPane;
    private JButton btnUpload;
    private JButton btnLoadImages;
    private JLabel lblImage;
    private JFileChooser fileChooser;
    private File selectedFile;
    private JPanel panelImages;
    private ArrayList<String> imageUrls = new ArrayList<String>();

    private Storage storage;
    private Firestore db;

    public static void main(String[] args) {

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Main window = new Main();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Main() {
        initialize();
        initFirebase();
    }
    
    private void saveImageDetailsToFirestore(String imageName, String year, String location, String date, String people, String originalImageName) throws IOException {
        // Create a new document with a unique ID
    	FileInputStream serviceAccount = new FileInputStream(SERVICE_ACCOUNT_KEY_PATH);
        FirebaseOptions options =  FirebaseOptions.builder()
        	    .setCredentials(GoogleCredentials.fromStream(serviceAccount))            	    
        	    .build();
        
        try {
        FirebaseApp.initializeApp(options);
        } catch (Exception aa) {
        	System.out.println("Already Exists");	
        }
        	
        db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("images").document();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("image_name", imageName);
        data.put("year", year);
        data.put("location", location);
        data.put("date", date);
        data.put("people", people);
        data.put("originalImageName", originalImageName);

        // Add the data to the document
        ApiFuture<WriteResult> result = docRef.set(data);
        try {
            // Wait for the write to complete
            result.get();
            System.out.println("Image details saved to Firestore");
        } catch (InterruptedException e) {
            System.err.println("Error saving image details to Firestore: " + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("Error saving image details to Firestore: " + e.getMessage());
        }
    }

    
    private void onUploadImageLogic() {
      	 // create a new JDialog
        final JDialog uploadDialog = new JDialog(frame, "Upload Image Details", true);
        uploadDialog.setLayout(new GridLayout(0, 2, 5, 5));
        uploadDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        uploadDialog.setSize(400, 200);

        // create the form labels and fields
        JLabel nameLabel = new JLabel("Image Name:");
        final JTextField nameField = new JTextField();
        JLabel yearLabel = new JLabel("Year:");
        final JTextField yearField = new JTextField();
        JLabel locationLabel = new JLabel("Location:");
        final JTextField locationField = new JTextField();
        JLabel dateLabel = new JLabel("Date:");
        final JTextField dateField = new JTextField();
        JLabel peopleLabel = new JLabel("People:");
        final JTextField peopleField = new JTextField();

        // add the labels and fields to the dialog
        uploadDialog.add(nameLabel);
        uploadDialog.add(nameField);
        uploadDialog.add(yearLabel);
        uploadDialog.add(yearField);
        uploadDialog.add(locationLabel);
        uploadDialog.add(locationField);
        uploadDialog.add(dateLabel);
        uploadDialog.add(dateField);
        uploadDialog.add(peopleLabel);
        uploadDialog.add(peopleField);

        // create the submit button
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // save the image details to Firestore
                String imageName = nameField.getText();
                String year = yearField.getText();
                String location = locationField.getText();
                String date = dateField.getText();
                String people = peopleField.getText();
                

                // close the dialog and show the file chooser
                uploadDialog.dispose();
                String originalName = showFileChooser();
                try {
					saveImageDetailsToFirestore(imageName, year, location, date, people, originalName);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
        uploadDialog.add(submitButton);

        // show the upload dialog
        uploadDialog.setVisible(true);
    }
    
    public class YearFilterWidget extends JComboBox<String> {
        private List<String> years;
        public  DefaultComboBoxModel<String> model;

        public YearFilterWidget(Firestore db) {
            super();
            this.years = new ArrayList<String>();
            this.years.add("All");

            // Query the Firestore collection for all unique years
            ApiFuture<QuerySnapshot> future = db.collection("images").orderBy("date").get();
            try {
                QuerySnapshot documents = future.get();
                for (QueryDocumentSnapshot document : documents) {
                    String year = document.getString("year");
                    if (!years.contains(year)) {
                        years.add(year);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error retrieving years from Firestore: " + e.getMessage());
            }

            // Add the years to the dropdown
            String[] yearArray = new String[years.size()];
            years.toArray(yearArray);
            model = new DefaultComboBoxModel<String>(yearArray);
            this.setModel(model);
        }

        public String getSelectedYear() {
            return (String) this.getSelectedItem();
        }
    }
    
    public class PeopleFilterWidget extends JComboBox<String> {
        private List<String> people;
        private DefaultComboBoxModel<String> model;

        public PeopleFilterWidget(Firestore db) {
            super();
            this.people = new ArrayList<String>();
            this.people.add("All");

            // Query the Firestore collection for all unique people
            ApiFuture<QuerySnapshot> future = db.collection("images").get();
            try {
                QuerySnapshot documents = future.get();
                for (QueryDocumentSnapshot document : documents) {
                    String peopleString = document.getString("people");
                    if (peopleString != null && !peopleString.isEmpty()) {
                        String[] peopleArray = peopleString.split(",\\s*");
                        for (String person : peopleArray) {
                            if (!people.contains(person)) {
                                people.add(person);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error retrieving people from Firestore: " + e.getMessage());
            }

            // Add the people to the dropdown
            String[] peopleArray = new String[people.size()];
            people.toArray(peopleArray);
            model = new DefaultComboBoxModel<String>(peopleArray);
            this.setModel(model);
        }

        public String getSelectedPerson() {
            return (String) this.getSelectedItem();
        }
    }

    
    
    
    public class LocationFilterWidget extends JPanel {

        public JComboBox<String> locationDropdown;
        private ArrayList<String> locations;
        


        public LocationFilterWidget(Firestore db, final String defaultLocation) {
            setLayout(new FlowLayout());
            JLabel locationLabel = new JLabel("Location:");
            add(locationLabel);
            locations = new ArrayList<String>();
            locationDropdown = new JComboBox<String>();

            
            add(locationDropdown);

            CollectionReference collection = db.collection("images");
            ApiFuture<QuerySnapshot> future = collection.get();
            locations.add("All");
            locationDropdown.addItem("All");
            try {
                QuerySnapshot documents = future.get();
                for (DocumentSnapshot document : documents) {
                    String location = document.getString("location");
                    if (location != null && !locations.contains(location)) {
                        locations.add(location);
                        locationDropdown.addItem(location);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting documents: " + e.getMessage());
            }
        }

        public String getSelectedLocation() {
            return (String) locationDropdown.getSelectedItem();
        }
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Image Gallery");
        frame.setBounds(100, 100, 800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(0, 0));
        frame.setContentPane(contentPane);

        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        contentPane.add(panelButtons, BorderLayout.NORTH);

        btnUpload = new JButton("Upload Image");
        btnUpload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	onUploadImageLogic();
            }
        });
        panelButtons.add(btnUpload);

        btnLoadImages = new JButton("Load Images");
        btnLoadImages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadImagesFromFirebaseStorage("All");
            }
        });
        panelButtons.add(btnLoadImages);
        

        
               

        panelImages = new JPanel();
        JScrollPane scrollPane = new JScrollPane(panelImages);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        //contentPane.add(filtersPanel, BorderLayout.CENTER);
        
    }

    private void initFirebase() {
        try {
            FileInputStream serviceAccount = new FileInputStream(SERVICE_ACCOUNT_KEY_PATH);
            

            storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID)
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build().getService();
            
            
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
	    private String getImageOriginalNameFromDownloadUrl(String downloadUrl) {
	        try {
	            // Parse the download URL to get the original image name
	            URL url = new URL(downloadUrl);
	            String[] pathComponents = url.getPath().split("/");
	            String originalImageName = pathComponents[pathComponents.length - 1];
	
	            // Remove any URL parameters or fragments from the original image name
	            int queryIndex = originalImageName.indexOf('?');
	            int fragmentIndex = originalImageName.indexOf('#');
	            int endIndex = originalImageName.length();
	            if (queryIndex != -1 && queryIndex < endIndex) {
	                endIndex = queryIndex;
	            }
	            if (fragmentIndex != -1 && fragmentIndex < endIndex) {
	                endIndex = fragmentIndex;
	            }
	            originalImageName = originalImageName.substring(0, endIndex);
	
	            return originalImageName;
	        } catch (MalformedURLException e) {
	            // Return null if the download URL is invalid
	            return null;
	        }
	    }

    
    private void loadImagesFromFirebaseStorage(String location) {
        // get a reference to the bucket
        Bucket bucket = storage.get(BUCKET_NAME);

        // get a page of blobs in the "images/" folder
        Page<Blob> blobs = bucket.list(BlobListOption.prefix("images/"), BlobListOption.currentDirectory());

        // create a new panel with a grid layout to hold the images
        JPanel panelImages = new JPanel(new GridLayout(0, 3, 5, 5));

        // iterate through the blobs and add each image to the panel
        for (Blob blob : blobs.getValues()) {
            // get the download URL for the blob
            final String downloadUrl = blob.getMediaLink();
            System.out.println(downloadUrl);

            // create an image icon from the download URL
            final ImageIcon icon = createScaledImageIcon(downloadUrl, 250, 250);

            // create a label to hold the image icon
            JLabel label = new JLabel(icon);

            // add a mouse listener to the label
            label.addMouseListener(new MouseListener() {    	

            	public void mouseClicked(java.awt.event.MouseEvent e) {
            	    final JFrame imageFrame = new JFrame();
            	    imageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            	    imageFrame.setSize(500, 500);

            	    // create a new label to hold the image in the bigger frame
            	    JLabel imageLabel = new JLabel(icon);
            	    imageLabel.setHorizontalAlignment(JLabel.CENTER);
            	    imageLabel.setVerticalAlignment(JLabel.CENTER);

            	    // add the label to the bigger frame and show it
            	    imageFrame.getContentPane().add(imageLabel);

            	    // create a download button
            	    JButton downloadButton = new JButton("Download");
            	    downloadButton.addActionListener(new ActionListener() {
            	        public void actionPerformed(ActionEvent e) {
            	            try {
            	                // open a connection to the image URL
            	                URL url = new URL(downloadUrl);
            	                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            	                conn.setRequestMethod("GET");
            	                conn.connect();

            	                // get the input stream for the connection
            	                InputStream in = conn.getInputStream();

            	                // create a file chooser to save the image
            	                JFileChooser fileChooser = new JFileChooser();
            	                fileChooser.setDialogTitle("Save Image");
            	                fileChooser.setSelectedFile(new File("image.jpg"));

            	                // show the save dialog and get the selected file
            	                int result = fileChooser.showSaveDialog(imageFrame);
            	                if (result == JFileChooser.APPROVE_OPTION) {
            	                    File file = fileChooser.getSelectedFile();

            	                    // write the input stream to the selected file
            	                    OutputStream out = new FileOutputStream(file);
            	                    byte[] buffer = new byte[4096];
            	                    int bytesRead;
            	                    while ((bytesRead = in.read(buffer)) != -1) {
            	                        out.write(buffer, 0, bytesRead);
            	                    }
            	                    out.close();
            	                    in.close();
            	                }
            	            } catch (IOException ex) {
            	                ex.printStackTrace();
            	            }
            	        }
            	    });
            	    
            	 // show the image details
            	    String originalImageName = getImageOriginalNameFromDownloadUrl(downloadUrl).replace("images%2F","");

            	    System.out.println("******");
            	    System.out.println(originalImageName);
                	FileInputStream serviceAccount = null;
					try {
						serviceAccount = new FileInputStream(SERVICE_ACCOUNT_KEY_PATH);
					} catch (FileNotFoundException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
                    FirebaseOptions options = null;
					try {
						options =  FirebaseOptions.builder()
							    .setCredentials(GoogleCredentials.fromStream(serviceAccount))            	    
							    .build();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
                    try {
                    FirebaseApp.initializeApp(options);
                    } catch (Exception aa) {
                    	System.out.println("Already Exists");
                    }
            	    db = FirestoreClient.getFirestore();
            	    CollectionReference imagesCollectionRef = db.collection("images");
            	    Query query = imagesCollectionRef.whereEqualTo("originalImageName", originalImageName);

            	    ApiFuture<QuerySnapshot> querySnapshot = query.get();
            	    QuerySnapshot snapshot = null;
					try {
						snapshot = querySnapshot.get();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (ExecutionException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
            	    if (!snapshot.isEmpty()) {
            	        DocumentSnapshot document = snapshot.getDocuments().get(0);
            	        String imageName = document.getString("image_name");
            	        String year = document.getString("year");
            	        String location = document.getString("location");
            	        String date = document.getString("date");
            	        String people = document.getString("people");

            	        JLabel nameLabel = new JLabel("Name: " + imageName);
            	        JLabel yearLabel = new JLabel("Year: " + year);
            	        JLabel locationLabel = new JLabel("Location: " + location);
            	        JLabel dateLabel = new JLabel("Date: " + date);
            	        JLabel peopleLabel = new JLabel("People: " + people);

            	        JPanel detailsPanel = new JPanel(new GridLayout(5, 1));
            	        detailsPanel.add(nameLabel);
            	        detailsPanel.add(yearLabel);
            	        detailsPanel.add(locationLabel);
            	        detailsPanel.add(dateLabel);
            	        detailsPanel.add(peopleLabel);

            	        imageFrame.getContentPane().add(detailsPanel, BorderLayout.NORTH);
            	    }


            	    // create a panel to hold the download button
            	    JPanel buttonPanel = new JPanel();
            	    buttonPanel.add(downloadButton);
            	    imageFrame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

            	    imageFrame.setVisible(true);
            	}



				public void mousePressed(java.awt.event.MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				public void mouseReleased(java.awt.event.MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				public void mouseEntered(java.awt.event.MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				public void mouseExited(java.awt.event.MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
            });

            // add the label to the panel
            panelImages.add(label);
        }

        // update the content pane to show the images
        contentPane.removeAll();
        btnUpload = new JButton("Upload Image");
        btnUpload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	onUploadImageLogic();
            }
        });
        
        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panelButtons.add(btnUpload);

        btnLoadImages = new JButton("Load Images");
        btnLoadImages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadImagesFromFirebaseStorage("All");
            }
        });
        
        
        
        /////////// Filters 
        JPanel filtersPanel = new JPanel();
        filtersPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        try {
        	FileInputStream serviceAccount = null;
			try {
				serviceAccount = new FileInputStream(SERVICE_ACCOUNT_KEY_PATH);
			} catch (FileNotFoundException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
            FirebaseOptions options = null;
			try {
				options =  FirebaseOptions.builder()
					    .setCredentials(GoogleCredentials.fromStream(serviceAccount))            	    
					    .build();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
          FirebaseApp.initializeApp(options);
          db = FirestoreClient.getFirestore();
        } catch (Exception aa) {
        	System.out.println("Already set");
        }
        YearFilterWidget yearFilter = new YearFilterWidget(db);
        filtersPanel.add(new JLabel("Year: "));
        filtersPanel.add(yearFilter);
        
       
       
        PeopleFilterWidget peopleFilter = new PeopleFilterWidget(db);
        filtersPanel.add(new JLabel("Person: "));
        filtersPanel.add(peopleFilter);
        
        
        final JComboBox<String> peopleDropdown = peopleFilter;
        
        
        final JComboBox<String> yearDropdown = yearFilter;
        
        LocationFilterWidget locationfilterWidget = new LocationFilterWidget(db,location);
        final JComboBox<String> locationDropdown = locationfilterWidget.locationDropdown;
        
        
        
        JButton applyButton = new JButton("Apply");
        
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedLocation = locationDropdown.getSelectedItem().toString();
                String selectedYear = yearDropdown.getSelectedItem().toString();
                String selectedPerson = peopleDropdown.getSelectedItem().toString();
                
                // Filter images by location
                Firestore db;
         
            	FileInputStream serviceAccount = null;
				try {
					serviceAccount = new FileInputStream(SERVICE_ACCOUNT_KEY_PATH);
				} catch (FileNotFoundException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
                FirebaseOptions options = null;
				try {
					options =  FirebaseOptions.builder()
						    .setCredentials(GoogleCredentials.fromStream(serviceAccount))            	    
						    .build();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
                try {
                FirebaseApp.initializeApp(options);
                } catch (Exception aa) {
                	System.out.println("Already Exists");
                }
        	    db = FirestoreClient.getFirestore();
                CollectionReference imagesCollection = db.collection("images");
                Query query = null; 
                if (!selectedLocation.equals("All")) {
                	query = imagesCollection.whereEqualTo("location", selectedLocation);
                }
 
               
                // If a year other than "All" is selected, add a whereEqualTo filter to the query
                if (!selectedYear.equals("All")) {
                	if (query != null) {
                		query = query.whereEqualTo("year", selectedYear);
                	}
                	else {
                		query = imagesCollection.whereEqualTo("year", selectedYear);
                	}
                }
                                
                if (query == null) {
                	 query = imagesCollection.whereNotEqualTo("year", -9999);
                }

               
                ApiFuture<QuerySnapshot> querySnapshot = query.get();
                List<QueryDocumentSnapshot> documents = null;
				try {
					documents = querySnapshot.get().getDocuments();
					//System.out.println("==========>>");
					//System.out.println(documents);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (ExecutionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

                
                // Create a new window to display filtered images
                JFrame frame = new JFrame("Location: " +  selectedLocation + ' ' + " Year: "+ selectedYear);
                frame.setMinimumSize(new Dimension(400, 200)); // set minimum size to 400x200 pixels

                JPanel panel = new JPanel(new GridLayout(0, 3, 5, 5));
                JScrollPane scrollPane = new JScrollPane(panel); // Add a scroll pane in case there are many images
               
                
                for (DocumentSnapshot document : documents) {
                    String imageUrl = document.getString("originalImageName");
                    String caption = document.getString("image_name");
                    
                    
                    //name filtering
                    String[] nameArray = document.getString("people").split(",");
                    if (!selectedPerson.equals("All")) {
                    	if (!Arrays.asList(nameArray).contains(selectedPerson)) {
                    		continue;
                    	}
                    }
                    
                    
                    imageUrl = "https://firebasestorage.googleapis.com/v0/b/javaimagegallery-68cd9.appspot.com/o/images%2F"+imageUrl+"?alt=media&token=4e5aafa0-f77a-448e-b19c-193dc6e60de1";
                    
                    // Create a new panel for each image
                    JPanel imagePanel = new JPanel(new BorderLayout());
                    JLabel imageLabel = null;
					imageLabel = new JLabel(createScaledImageIcon(imageUrl, 250, 250));
                    JLabel captionLabel = new JLabel(caption);
                    imagePanel.add(imageLabel, BorderLayout.CENTER);
                    imagePanel.add(captionLabel, BorderLayout.SOUTH);
                    
                    // Add image panel to the grid layout
                    panel.add(imagePanel);
                }
                
                // Add scroll pane to the frame and show the window
                frame.add(scrollPane);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
         });

        
        filtersPanel.add(locationfilterWidget);
        filtersPanel.add(applyButton);

        //////////
        
        
      
        
        panelButtons.add(btnLoadImages);
        contentPane.add(panelButtons, BorderLayout.NORTH);
        contentPane.add(panelImages, BorderLayout.CENTER);
        contentPane.add(filtersPanel, BorderLayout.SOUTH);
        contentPane.revalidate();
        contentPane.repaint();
        frame.setContentPane(contentPane);
    }

    private ImageIcon createScaledImageIcon(String imageUrl, int width, int height) {
        try {
            // download the image from the URL
            URL url = new URL(imageUrl);
            BufferedImage img = ImageIO.read(url);

            // scale the image to the desired size
            Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

            // create and return the image icon
            return new ImageIcon(scaledImg);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    
    private String uploadImageToFirebaseStorage(File file) {
        String imageName = UUID.randomUUID().toString() + ".jpg"; // generate a unique image name
        System.out.println(imageName);

        // create a bucket reference and upload the file to Firebase Storage
        BlobId blobId = BlobId.of(BUCKET_NAME, "images/" + imageName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/jpeg").build();
        try {
            storage.create(blobInfo, new FileInputStream(file));
            System.out.println("Image uploaded successfully to Firebase Storage.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return imageName;
    }

    private void displaySelectedImage(File file) {
        Image img = new ImageIcon(file.getAbsolutePath()).getImage();
        int imgWidth = lblImage.getWidth();
        int imgHeight = lblImage.getHeight();
        Image scaledImg = img.getScaledInstance(imgWidth, imgHeight, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaledImg);
        lblImage.setIcon(icon);
    } 




    private String showFileChooser() {
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        int result = fileChooser.showOpenDialog(contentPane);
        System.out.println("=======>>>>!!");
        System.out.println(result);
       if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            //displaySelectedImage(selectedFile);
            System.out.println("=======>>>>");
            return uploadImageToFirebaseStorage(selectedFile);
        }
       return selectedFile.toString();
    }
    
    
    
}
