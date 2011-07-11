package javaxt.io;
import java.awt.image.*;
import java.awt.*;
import java.io.*;
import javax.imageio.*;
//import javax.imageio.stream.*;
import java.util.*;
import com.sun.image.codec.jpeg.*;
import java.awt.geom.AffineTransform;
import java.awt.color.ColorSpace;

//import java.awt.geom.*;
//Imports for JP2
//import javax.media.jai.RenderedOp;
//import com.sun.media.imageio.plugins.jpeg2000.J2KImageReadParam;


//******************************************************************************
//**  Image Utilities - By Peter Borissow
//******************************************************************************
/**
 *   Used to open, resize, rotate, crop and save images.
 *
 ******************************************************************************/

public class Image {
    
    private BufferedImage bufferedImage = null; 
    private int width = 0;
    private int height = 0;
    private Vector corners = null;
    
    private float outputQuality = 1f; //0.9f; //0.5f;
    
    private Graphics2D g2d = null;
    
    public static String[] InputFormats = getFormats(ImageIO.getReaderFormatNames());
    public static String[] OutputFormats = getFormats(ImageIO.getWriterFormatNames());


  //**************************************************************************
  //** Creates a new instance of image
  //**************************************************************************
  /**  Creates a new instance of image using an existing image */
    
    public Image(String PathToImageFile){
        this(new java.io.File(PathToImageFile));
    }
    
    public Image(java.io.File File){
        createBufferedImage(File);
    }
    
    public Image(javaxt.io.File File){
        this(File.toFile());
    }
    
    public Image(java.io.InputStream InputStream){
        createBufferedImage(InputStream);
    }
    
    public Image(byte[] byteArray){
        this(new ByteArrayInputStream(byteArray));
    }
    
    public Image(int Width, int Height){
        this.width = Width;
        this.height = Height;
        this.bufferedImage = 
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        this.g2d = getGraphics();
    }
    
    public Image(BufferedImage bufferedImage){
        this.bufferedImage = bufferedImage;
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
    }


    public Image(RenderedImage img) {
        if (img instanceof BufferedImage) {
            this.bufferedImage = (BufferedImage) img;
            this.width = bufferedImage.getWidth();
            this.height = bufferedImage.getHeight();
        }
        else{
            java.awt.image.ColorModel cm = img.getColorModel();
            int width = img.getWidth();
            int height = img.getHeight();
            java.awt.image.WritableRaster raster =
                cm.createCompatibleWritableRaster(width, height);
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            java.util.Hashtable properties = new java.util.Hashtable();
            String[] keys = img.getPropertyNames();
            if (keys!=null) {
                for (int i = 0; i < keys.length; i++) {
                    properties.put(keys[i], img.getProperty(keys[i]));
                }
            }
            BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
            img.copyData(raster);
            this.bufferedImage = result;
            this.width = bufferedImage.getWidth();
            this.height = bufferedImage.getHeight();
        }
    }


  //**************************************************************************
  //** Image
  //**************************************************************************
  /**  Used to create a new image from text. */

    public Image(String text, String fontName, int fontSize, int r, int g, int b){
        this(text, new Font(fontName, Font.TRUETYPE_FONT, fontSize), r,g,b);
    }

    public Image(String text, Font font, int r, int g, int b){

      //Get Font Metrics
        Graphics2D t = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        t.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        //Font font = new Font(fontName, Font.TRUETYPE_FONT, fontSize);

        FontMetrics fm = t.getFontMetrics(font);
        this.width = fm.stringWidth(text);
        this.height = fm.getHeight();
        int descent = fm.getDescent();

        t.dispose();


      //Create Image
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

      //Add Text
        float alpha = 1.0f; //Set alpha.  0.0f is 100% transparent and 1.0f is 100% opaque.
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(new Color(r, g, b));
        g2d.setFont(font);

        g2d.drawString(text, 0, height-descent);

    }
    
    
  //**************************************************************************
  //** setBackgroundColor
  //**************************************************************************
  /** Used to set the background color. Creates an image layer and inserts it
   *  under the existing graphic. This method should only be called once.
   */
    public void setBackgroundColor(int r, int g, int b){
        /*
        Color org = g2d.getColor();
        g2d.setColor(new Color(r,g,b));
        g2d.fillRect(1,1,width-2,height-2); //g2d.fillRect(0,0,width,height);
        g2d.setColor(org);
        */

        int imageType = bufferedImage.getType();
        if (imageType == 0) {
            imageType = BufferedImage.TYPE_INT_ARGB;
        }

        BufferedImage bi = new BufferedImage(width, height, imageType);
        Graphics2D g2d = bi.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2d.setColor(new Color(r,g,b));
        g2d.fillRect(0,0,width,height);

        java.awt.Image img = bufferedImage;
        g2d.drawImage(img, 0, 0, null);

        this.bufferedImage = bi;

        g2d.dispose();



    }
    
    
    
  //**************************************************************************
  //** getInputFormats
  //**************************************************************************
  /**  Used to retrieve a list of supported input (read) formats. */
    
    public String[] getInputFormats(){
        return getFormats(ImageIO.getReaderFormatNames());
    }
    
    

    
  //**************************************************************************
  //** getOutputFormats
  //**************************************************************************
  /**  Used to retrieve a list of supported output (write) formats. */
    
    public String[] getOutputFormats(){
        return getFormats(ImageIO.getWriterFormatNames());
    }
    
    
  //**************************************************************************
  //** getFormats
  //**************************************************************************
  /**  Used to trim the list of formats. */
    
    private static String[] getFormats(String[] inputFormats){

      //Build a unique list of file formats
        HashSet<String> formats = new HashSet<String> ();
        for (int i=0; i<inputFormats.length; i++){
             String format = inputFormats[i].toUpperCase();
             if (format.contains("JPEG") && format.contains("2000")){
                 formats.add("JP2");
                 formats.add("J2C");
                 formats.add("J2K");
                 formats.add("JPX");
             }
             else{
                 formats.add(format);
             }
        }
        
      //Convert the HashSet into an Array
        int x = 0;
        inputFormats = new String[formats.size()];
        Iterator<String> format = formats.iterator();
        while(format.hasNext()){
            inputFormats[x] = format.next();
            x++;
        }

      //Sort and return the array
        java.util.Collections.sort(java.util.Arrays.asList(inputFormats));
        return inputFormats;
    }
    

    
  //**************************************************************************
  //** getWidth
  //**************************************************************************
  /**  Returns the width of the image, in pixels. */
    
    public int getWidth(){ return bufferedImage.getWidth(); }
    
    
  //**************************************************************************
  //** getHeight
  //**************************************************************************
  /**  Returns the height of the image, in pixels. */
    
    public int getHeight(){ return bufferedImage.getHeight(); }
    
    
  //**************************************************************************
  //** getGraphics
  //**************************************************************************
    
    private Graphics2D getGraphics(){
        if (g2d==null){
            g2d = this.bufferedImage.createGraphics();
            
          //Enable anti-alias
            RenderingHints rhints = g2d.getRenderingHints();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
        }
        return g2d;
    }
    
  //**************************************************************************
  //** addText
  //**************************************************************************
  /**  Used to add text to the image at a given position. */
    
    public void addText(String text, int x, int y){
        
      //Create Graphics
        g2d = getGraphics();
             
        g2d.setColor(Color.BLACK); 
        g2d.setFont (new Font ("SansSerif",Font.TRUETYPE_FONT,12));
        g2d.drawString(text, x, y);
        
      //Graphics context no longer needed so dispose it
        //g2d.dispose();
    }
    
    
  //**************************************************************************
  //** addPoint
  //**************************************************************************
  /**  Simple drawing function used to set color of a specific pixel in the 
   *   image. 
   */
    
    public void addPoint(int x, int y, int r, int g, int b){
        g2d = getGraphics();
        Color org = g2d.getColor();
        g2d.setColor(new Color(r,g,b));
        g2d.fillRect(x,y,1,1);
        g2d.setColor(org);
    }
    
    
  //**************************************************************************
  //** getColor
  //**************************************************************************
  /**  Used to retrieve the color (ARGB) values for a specific pixel in the 
   *   image. Returns a java.awt.Color object. Note that input x,y values are
   *   relative to the upper left corner of the image, starting at 0,0. 
   */
    
    public Color getColor(int x, int y){
        return new Color(bufferedImage.getRGB(x, y));
    }
    
    
  //**************************************************************************
  //** addImage
  //**************************************************************************
  /**  Used to add an image "overlay" to the existing image at a given 
   *   position. This method can also be used to create image mosiacs.
   */
    
    public void addImage(BufferedImage in, int x, int y, boolean expand){ 
        
        
        int x2 = 0;
        int y2 = 0;
        
        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();
        
        
        if (expand){

          //Update Width and Horizontal Position of the Original Image
            if (x<0) {
                w = w + -x;
                if (in.getWidth()>w){
                    w = w + (in.getWidth()-w);
                }
                x2 = -x;
                x = 0;
            }
            else if (x>w) {
                w = (w + (x-w)) + in.getWidth();
            }
            else{
                if ((x+in.getWidth())>w){
                    w = w + ((x+in.getWidth())-w);
                }
            }
            
          //Update Height and Vertical Position of the Original Image
            if (y<0){
                h = h + -y;
                if (in.getHeight()>h){
                    h = h + (in.getHeight()-h);
                }
                y2 = -y;
                y = 0;
            }
            else if(y>h){
                h = (h + (y-h)) + in.getHeight();
            }
            else{
                if ((y+in.getHeight())>h){
                    h = h + ((y+in.getHeight())-h);
                }
            }

        }
        
            
      //Create new image "collage"
        if (w>bufferedImage.getWidth() || h>bufferedImage.getHeight()){
            int imageType = bufferedImage.getType();
            if (imageType == 0) {
              imageType = BufferedImage.TYPE_INT_ARGB;
            }
            BufferedImage bi = new BufferedImage(w, h, imageType);
            Graphics2D g2d = bi.createGraphics();
            java.awt.Image img = bufferedImage;
            g2d.drawImage(img, x2, y2, null);
            img = in;
            g2d.drawImage(img, x, y, null);
            bufferedImage = bi;
            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
        }
        else{
            Graphics2D g2d = bufferedImage.createGraphics();
            java.awt.Image img = in;
            g2d.drawImage(img, x, y, null);
            g2d.dispose();
        }
            
    }
    
  //**************************************************************************
  //** addImage
  //**************************************************************************
  /**  Used to add an image "overlay" to the existing image at a given 
   *   position. This method can also be used to create image mosiacs.
   */    
    public void addImage(javaxt.io.Image in, int x, int y, boolean expand){ 
        addImage(in.getBufferedImage(),x,y,expand);
    }

    
    
  //**************************************************************************
  //** createBufferedImage
  //**************************************************************************
    
    private void createBufferedImage(java.io.File File){
        try{
            
            //long startTime = getStartTime();
            
            bufferedImage = ImageIO.read(File); 
            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
            
            if (bufferedImage.getType()==BufferedImage.TYPE_BYTE_INDEXED){
                
                BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = bi.createGraphics();
                java.awt.Image img = bufferedImage;
                g2d.drawImage(img, 0, 0, null);
                bufferedImage = bi;
            }
            
            
            //System.out.println("Input image is " + width + "x" + height + "...");
            //System.out.println("Opened file in " + getEllapsedTime(startTime) + " ms...");
            
        }
        catch(Exception e){
            printError(e);
        }
    }
    
    private void createBufferedImage(java.io.InputStream input){
        try{
            
            long startTime = getStartTime();
            
            bufferedImage = ImageIO.read(input); 
            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
            input.close();
            
            
            //System.out.println("Input image is " + width + "x" + height + "...");
            //System.out.println("Opened file in " + getEllapsedTime(startTime) + " ms...");
            
        }
        catch(Exception e){
            printError(e);
        }
    }
    
    
    
  //**************************************************************************
  //** Rotate
  //**************************************************************************
  /**  Used to rotate the image (clockwise). Rotation angle is specified in 
   *   degrees relative to the top of the image. 
   */
    
    public void rotate(double Degrees){
        
      //Define Image Center (Axis of Rotation)
        int cx = width/2;
        int cy = height/2;

      //create an array containing the corners of the image (TL,TR,BR,BL)
        int[] corners = { 0, 0, width, 0, width, height, 0, height };
        
      //Define bounds of the image
        int minX, minY, maxX, maxY;
        minX = maxX = cx;
        minY = maxY = cy;
        double theta = Math.toRadians(Degrees);
        for (int i=0; i<corners.length; i+=2){
            
           //Rotates the given point theta radians around (cx,cy)
             int x = getInt( (Math.cos(theta)*(corners[i]-cx) -
                              Math.sin(theta)*(corners[i+1]-cy)+cx) );
             int y = getInt( (Math.sin(theta)*(corners[i]-cx) +
                              Math.cos(theta)*(corners[i+1]-cy)+cy) );

           //Update our bounds
             if(x>maxX) maxX = x;
             if(x<minX) minX = x;
             if(y>maxY) maxY = y;
             if(y<minY) minY = y;
        }

        
      //Update Image Center Coordinates
        cx = (int)(cx-minX);
        cy = (int)(cy-minY);

      //Create Buffered Image
        BufferedImage result = new BufferedImage(maxX-minX, maxY-minY, 
                                                 BufferedImage.TYPE_INT_ARGB);
        
      //Create Graphics
        Graphics2D g2d = result.createGraphics();
        
      //Enable anti-alias and Cubic Resampling
        RenderingHints rhints = g2d.getRenderingHints();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                             RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        
      //Rotate the image
        AffineTransform xform = new AffineTransform();
        xform.rotate(theta,cx,cy);
	g2d.setTransform(xform);
	g2d.drawImage(bufferedImage,-minX,-minY,null);
        g2d.dispose();
        
      //Update Class Variables
        this.bufferedImage = result;
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
        
      //Delete Heavy Objects
        result = null;
        xform = null;

    }
    
  //**************************************************************************
  //** getInt
  //**************************************************************************  
  /**  Converts a double to an integer value, rounding the double as needed */
    
    private int getInt(double value){
        return (int) Math.round(value);
    }
    
  //**************************************************************************
  //** Rotate Clockwise
  //**************************************************************************
  /**  Rotates the image 90 degrees clockwise */
    
    public void rotateClockwise(){
        rotate(90);
    }
    
  //**************************************************************************
  //** Rotate Counter Clockwise
  //**************************************************************************
  /**  Rotates the image -90 degrees */
    
    public void rotateCounterClockwise(){
        rotate(-90);
    }
    
    
  //**************************************************************************
  //** setWidth
  //**************************************************************************
  /**  Resizes the image to a given width. The original aspect ratio is 
   *   maintained. 
   */
    
    public void setWidth(int Width){
        double ratio = (double)Width/(double)width;
        
        double dw = width * ratio;
        double dh = height * ratio;

        int outputWidth =  (int)Math.round(dw); 
        int outputHeight = (int)Math.round(dh); 
        
        resize(outputWidth,outputHeight);
    }
    
    
  //**************************************************************************
  //** setHeight
  //**************************************************************************
  /**  Resizes the image to a given height. The original aspect ratio is 
   *   maintained. 
   */
    
    public void setHeight(int Height){
        double ratio = (double)Height/(double)height;
        
        double dw = width * ratio;
        double dh = height * ratio;

        int outputWidth =  (int)Math.round(dw); 
        int outputHeight = (int)Math.round(dh); 
        
        resize(outputWidth,outputHeight);
    }
    
    
  //**************************************************************************
  //** Resize (Overloaded Member)
  //**************************************************************************
  /**  Used to resize an image. Does NOT automatically retain the original 
   *   aspect ratio. 
   */
    
    public void resize(int Width, int Height){
        resize(Width,Height,false);
    }
    
  //**************************************************************************
  //** Resize
  //**************************************************************************
  /**  Used to resize an image. Provides the option to maintain the original 
   *   aspect ratio (relative to the output width).
   */
    
    public void resize(int Width, int Height, boolean maintainRatio){
        
        long startTime = getStartTime();
        
        int outputWidth = Width;
        int outputHeight = Height;
        
        if (maintainRatio){

            double ratio = 0;

            if (width>height){
                ratio = (double)Width/(double)width;
            }
            else{
                ratio = (double)Height/(double)height;
            }

            double dw = width * ratio;
            double dh = height * ratio;

            outputWidth =  (int)Math.round(dw); 
            outputHeight = (int)Math.round(dh); 

            if (outputWidth>width || outputHeight>height){
                outputWidth=width;
                outputHeight=height;
            }
        }
        
        
      //Resize the image (create new buffered image)
        java.awt.Image outputImage = bufferedImage.getScaledInstance(outputWidth, outputHeight, BufferedImage.SCALE_AREA_AVERAGING);
        BufferedImage bi = new BufferedImage(outputWidth, outputHeight, getImageType());
        Graphics2D g2d = bi.createGraphics( );
        g2d.drawImage(outputImage, 0, 0, null);
        g2d.dispose();

        this.bufferedImage = bi;
        this.width = outputWidth;
        this.height = outputHeight;
        
        outputImage = null;
        bi = null;
        g2d = null;
        
        //System.out.println("Resized image in " + getEllapsedTime(startTime) + " ms...");
    }
    
    
    
  //**************************************************************************
  //** Set/Update Corners (Skew)
  //**************************************************************************
  /**  Used to skew an image by updating the corner coordinates. Coordinates are 
   *   supplied in clockwise order starting from the upper left corner.
   */
    public void setCorners(float x0, float y0,  //UL
                           float x1, float y1,  //UR
                           float x2, float y2,  //LR
                           float x3, float y3){ //LL
        
        Skew skew = new Skew(this.bufferedImage);
        this.bufferedImage = skew.setCorners(x0,y0,x1,y1,x2,y2,x3,y3);
        
        if (corners==null) corners = new Vector();
        else corners.clear();        
        corners.add((Float)x0); corners.add((Float)y0);
        corners.add((Float)x1); corners.add((Float)y1);
        corners.add((Float)x2); corners.add((Float)y2);
        corners.add((Float)x3); corners.add((Float)y3);
    }
    
    
  //**************************************************************************
  //** Get Corners 
  //**************************************************************************
  /**  Used to retrieve the corner coordinates of the image. Coordinates are 
   *   supplied in clockwise order starting from the upper left corner. This 
   *   information is particularely useful for generating drop shadows, inner 
   *   and outer glow, and reflections.
   *   NOTE: Coordinates are not updated after resize(), rotate(), or addImage()
   */
    public float[] getCorners(){
        
        if (corners==null){
            float w = getWidth();
            float h = getHeight();
            corners = new Vector();
            corners.add((Float)0f); corners.add((Float)0f);
            corners.add((Float)w); corners.add((Float)0f);
            corners.add((Float)w); corners.add((Float)h);
            corners.add((Float)0f); corners.add((Float)h);
        }
        
        Object[] arr = corners.toArray();
        float[] ret = new float[arr.length];
        for (int i=0; i<arr.length; i++){
            Float f = (Float) arr[i];
             ret[i] = f.floatValue();
        }
        return ret;
    }
    
    
  //**************************************************************************
  //** Sharpen
  //**************************************************************************
  /**  Used to sharpen the image using a 3x3 kernal. */
    
    public void sharpen(){
        
      //define kernal
        Kernel kernel = new Kernel(3, 3,
            new float[] {
             0.0f, -0.2f,  0.0f,
            -0.2f,  1.8f, -0.2f,
             0.0f, -0.2f,  0.0f });

      //apply convolution
        BufferedImage out = new BufferedImage(width, height, getImageType());
        BufferedImageOp op = new ConvolveOp(kernel);
        out = op.filter(bufferedImage, out); 
        
      //replace 2 pixel border created via convolution
        java.awt.Image overlay = out.getSubimage(2,2,width-4,height-4);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(overlay,2,2,null);
        g2d.dispose();
        
    }
    
    
  //**************************************************************************
  //** Desaturate
  //**************************************************************************
  /**  Used to completely desaturate an image (creates a gray-scale image). */
    
    public void desaturate(){
         bufferedImage = desaturate(bufferedImage);
    }
    
    
  //**************************************************************************
  //** Desaturate
  //**************************************************************************
  /**  Used to desaturate an image by a specified percentage (expressed as 
   *   a double or float). The larger the percentage, the greater the 
   *   desaturation and the "grayer" the image. Valid ranges are from 0-1.
   */
    
    public void desaturate(double percent){
        float alpha = (float) (percent);
        java.awt.Image overlay = desaturate(bufferedImage);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(overlay,0,0,null);
        g2d.dispose();
    }
    
    
  //**************************************************************************
  //** Desaturate (Private Function)
  //**************************************************************************
  /**  Convenience function called by the other 2 desaturation methods. */
    
    private BufferedImage desaturate(BufferedImage in){
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), getImageType(in) );
        BufferedImageOp op = new ColorConvertOp(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return op.filter(in, out);
    }
    
    
    
  //**************************************************************************
  //** setOpacity
  //**************************************************************************
    
    public void setOpacity(double percent){
        if (percent>1) percent=percent/100;
        float alpha = (float) (percent);
        int imageType = bufferedImage.getType();
        if (imageType == 0) {
          imageType = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage out = new BufferedImage(width,height,imageType);
        Graphics2D g2d = out.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(bufferedImage,0,0,null);
        g2d.dispose();
        bufferedImage = out;
    }
    
  //**************************************************************************
  //** Flip (Horizonal)
  //**************************************************************************
  /**  Used to flip an image along it's y-axis (horizontal). Vertical flipping 
   *   is supported via the rotate method (i.e. rotate +/-180).
   */
    
    public void flip(){
        BufferedImage out = new BufferedImage(width, height, getImageType());
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-bufferedImage.getWidth(), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
        bufferedImage = op.filter(bufferedImage, out);
    }
    
    

    
  //**************************************************************************
  //** Crop
  //**************************************************************************
  /**  Used to subset or crop an image. */
    
    public void crop(int x, int y, int width, int height){
        bufferedImage = bufferedImage.getSubimage(x,y,width,height);
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
    }
    
    
  //**************************************************************************
  //** getBufferedImage
  //**************************************************************************
  /**  Returns a java.awt.image.BufferedImage copy of the current image. */
    
    public BufferedImage getBufferedImage(){
        try{
            //return ImageIO.read(new ByteArrayInputStream(getByteArray()));
            return bufferedImage;
        }
        catch(Exception e){
            System.out.println(e);
            //printError(e);
            return null;
        }
    }
    
    
  //**************************************************************************
  //** getImage
  //**************************************************************************
  /**  Returns a java.awt.Image copy of the current image. */
    
    public java.awt.Image getImage(){
        return getBufferedImage();
    }
    
  //**************************************************************************
  //** getImage
  //**************************************************************************
  /**  Returns a java.awt.image.RenderedImage copy of the current image. */
    
    public java.awt.image.RenderedImage getRenderedImage(){
        return getBufferedImage();
    }
    
    
  //**************************************************************************
  //** getBufferedImage
  //**************************************************************************
  /**  Used to retrieve a scaled copy of the current image. */
    
    public BufferedImage getBufferedImage(int width, int height, boolean maintainRatio){
        Image image = new Image(getBufferedImage());
        image.resize(width,height,maintainRatio);
        return image.getBufferedImage();
    }
    
    
  //**************************************************************************
  //** getByteArray
  //**************************************************************************
    
    public byte[] getByteArray(){
        byte[] rgb = null;
        try{
            
            
            if (outputQuality>=0f && outputQuality<=1f)
                rgb = getJPEGByteArray(outputQuality);
            else
                rgb = getJPEGByteArray(0.7f);
            
            
            /*
            //FileInputStream InputStream = new FileInputStream(File);
            ImageInputStream InputStream = ImageIO.createImageInputStream(bufferedImage);
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int x=0;
            while((x=InputStream.read(b,0,1024))>-1) {
                bas.write(b,0,x);
            }
            bas.close();
            rgb = bas.toByteArray();
            */  
                    
        }
        catch(Exception e){e.printStackTrace();}
        return rgb;
    }
    
  //**************************************************************************
  //** getByteArray
  //**************************************************************************
    
    public byte[] getByteArray(String format){
        byte[] rgb = null;
        
        format = format.toLowerCase();
        if (format.startsWith("image/")){
            format = format.substring(format.indexOf("/")+1);
        }
        
        try{
            if (isJPEG(format)){
                rgb = getJPEGByteArray(outputQuality);
            }
            else{
                ByteArrayOutputStream bas = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, format.toLowerCase(), bas);
                rgb = bas.toByteArray();
            }
        }
        catch(Exception e){}
        return rgb;
    }
    
    
    
  //**************************************************************************
  //** saveAs
  //**************************************************************************
  /**  Exports the image to a file. Output format is determined by the output 
   *   file extension. 
   */
    
    public void saveAs(javaxt.io.File OutputFile){
        saveAs(OutputFile.getFile());
    }
    
  //**************************************************************************
  //** saveAs
  //**************************************************************************
  /**  Exports the image to a file. Output format is determined by the output 
   *   file extension. 
   */
    
    public void saveAs(String PathToImageFile){
        saveAs(new java.io.File(PathToImageFile));
    }
    
    
  //**************************************************************************
  //** saveAs
  //**************************************************************************
  /**  Exports the image to a file. Output format is determined by the output 
   *   file extension. 
   */
    
    public void saveAs(java.io.File OutputFile){
        try{
          //Create output directory
            OutputFile.getParentFile().mkdirs();
            
          //Write buffered image to disk
            String FileExtension = getExtension(OutputFile.getName()).toLowerCase();
            if (isJPEG(FileExtension)){
                FileOutputStream output = new FileOutputStream(OutputFile);
                output.write(getJPEGByteArray(outputQuality));
                output.close();
            }
            else{
                RenderedImage rendImage = bufferedImage; 
                if (isJPEG2000(FileExtension)){
                    ImageIO.write(rendImage, "JPEG 2000", OutputFile);
                }
                else{
                    ImageIO.write(rendImage, FileExtension, OutputFile);
                }
                rendImage = null;
            }
            //System.out.println("Output image is " + width + "x" + height + "...");
        }
        catch(Exception e){
            printError(e);
        }
    }
    
    
    /*
    public void setCacheDirectory(java.io.File cacheDirectory){
        try{
            if (cacheDirectory.isFile()){ 
                cacheDirectory = cacheDirectory.getParentFile();
            }
            cacheDirectory.mkdirs();
            ImageIO.setUseCache(true);
            this.cacheDirectory = cacheDirectory;
        }
        catch(Exception e){
            this.cacheDirectory = null;
        }
    }
    
    public java.io.File getCacheDirectory(){
        return cacheDirectory;
    }
    */
    
  //**************************************************************************
  //** setOutputQuality
  //**************************************************************************
  /**  Used to set the output quality/compression ratio. Only applies when 
   *   creating JPEG images. Applied only when writing the image to a file or 
   *   byte array.
   */
    
    public void setOutputQuality(double percentage){
        if (percentage>1&&percentage<=100) percentage=percentage/100;
        float q = (float) percentage;
        if (q==1f) q = 1.2f;
        if (q>=0f && q<=1.2f) outputQuality = q;
    }
    
    
    
    
    
    
  //**************************************************************************
  //** isJPEG
  //**************************************************************************
  /**  Used to determine whether to create a custom jpeg compressed image   */
    
    private boolean isJPEG(String FileExtension){
        FileExtension = FileExtension.trim().toLowerCase();
        if (FileExtension.equals("jpg") || 
            FileExtension.equals("jpeg") || 
            FileExtension.equals("jpe") ){
            return true;
        }
        return false;
    }
    
    
  //**************************************************************************
  //** isJPEG2000
  //**************************************************************************
  /**  Used to determine whether to create a custom jpeg compressed image   */
    
    private boolean isJPEG2000(String FileExtension){
        FileExtension = FileExtension.trim().toLowerCase();
        if (FileExtension.equals("jp2") || 
            FileExtension.equals("jpc") || 
            FileExtension.equals("j2k") ||
            FileExtension.equals("jpx") ){
            return true;
        }
        return false;
    }
    
    
  //**************************************************************************
  //** getJPEG
  //**************************************************************************
  /**  Used to create a JPEG compressed byte array. */
    
    private byte[] getJPEGByteArray(float outputQuality) throws IOException {
        if (outputQuality>=0f && outputQuality<=1.2f) {
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            BufferedImage bi = bufferedImage;
            int t = bufferedImage.getTransparency();
            
            //if (t==BufferedImage.BITMASK) System.out.println("BITMASK");
            //if (t==BufferedImage.OPAQUE) System.out.println("OPAQUE");
            
            if (t==BufferedImage.TRANSLUCENT){
                bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D biContext = bi.createGraphics();
                biContext.drawImage ( bufferedImage, 0, 0, null );
            }
            
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(bas);
            JPEGEncodeParam params = JPEGCodec.getDefaultJPEGEncodeParam(bi);
            params.setQuality(outputQuality, true); //true
            params.setHorizontalSubsampling(0,2);
            params.setVerticalSubsampling(0,2);
            encoder.encode(bi, params);
            bas.flush();
            return bas.toByteArray();
        }
        else{
            return getByteArray();
        }
    }
    
    
    
    
  //**************************************************************************
  //** getImageType
  //**************************************************************************
    
    private int getImageType(){
        return getImageType(this.bufferedImage);
    }
    
    private int getImageType(BufferedImage bufferedImage){
        int i = bufferedImage.getType();
        if (i<=0) i = BufferedImage.TYPE_INT_ARGB; //<- is this ok?
        return i;
    }
    
    
  //**************************************************************************
  //** getExtension
  //**************************************************************************
  /**  Returns the file extension for a given file name, if one exists. */
    
    private String getExtension(String FileName){
        if (FileName.contains((CharSequence) ".")){
            return FileName.substring(FileName.lastIndexOf(".")+1,FileName.length());
        }
        else{
            return "";
        }
    }
    
    
  //**************************************************************************
  //** equals
  //**************************************************************************
  /**  Used to compare this image to another. If the ARGB values match, this 
   *   method will return true. 
   */
    
    public boolean equals(Object obj){
        if (obj!=null){
            if (obj instanceof javaxt.io.Image){
                javaxt.io.Image image = (javaxt.io.Image) obj;
                if (image.getWidth()==this.getWidth() && 
                    image.getHeight()==this.getHeight())
                {
                    
                  //Iterate through all the pixels in the image and compare RGB values
                    for (int i=0; i<image.getWidth(); i++){
                         for (int j=0; j<image.getHeight(); j++){
                             
                              if (!image.getColor(i,j).equals(this.getColor(i,j))){
                                  return false;
                              }
                         }
                         
                    }
                    
                    return true;
                    
                }
                
            }
        }
        return false;
    }
    
    
    
    
    
  //**************************************************************************
  //** printError
  //**************************************************************************
    
    private void printError(Exception e){
        System.out.println(e.toString());
        StackTraceElement[] arr = e.getStackTrace();
        for (int i=0; i<arr.length; i++){
             System.out.println(arr[i].toString());
        }
    }
    
    
    private long getStartTime(){
        return java.util.Calendar.getInstance().getTimeInMillis();
    }
    
    private long getEllapsedTime(long StartTime){
       long endTime = java.util.Calendar.getInstance().getTimeInMillis();
       return endTime-StartTime;  
    }
    
    
    
    
    
    
    
    

   //***************************************************************************
   //**  Skew Class
   //***************************************************************************
   /**
    *   Used to skew an image. Adapted from 2 image processing classes developed 
    *   by Jerry Huxtable (http://www.jhlabs.com) and released under 
    *   the Apache License, Version 2.0.
    *
    ***************************************************************************/

    private class Skew {


        public final static int ZERO = 0;
        public final static int CLAMP = 1;
        public final static int WRAP = 2;

        public final static int NEAREST_NEIGHBOUR = 0;
        public final static int BILINEAR = 1;

        protected int edgeAction = ZERO;
        protected int interpolation = BILINEAR;

        protected Rectangle transformedSpace;
        protected Rectangle originalSpace;

        private float x0, y0, x1, y1, x2, y2, x3, y3;
        private float dx1, dy1, dx2, dy2, dx3, dy3;
        private float A, B, C, D, E, F, G, H, I;


        private BufferedImage src;
        private BufferedImage dst;

      //**************************************************************************
      //** Constructor
      //**************************************************************************
      /**  Creates a new instance of Skew */

        public Skew(BufferedImage src) {
            this.src = src;
            this.dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        }

        public Skew(javaxt.io.Image src) {
            this(src.getBufferedImage());
        }


        public BufferedImage setCorners(float x0, float y0, 
                                        float x1, float y1, 
                                        float x2, float y2, 
                                        float x3, float y3) {
                this.x0 = x0;
                this.y0 = y0;
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
                this.x3 = x3;
                this.y3 = y3;

                dx1 = x1-x2;
                dy1 = y1-y2;
                dx2 = x3-x2;
                dy2 = y3-y2;
                dx3 = x0-x1+x2-x3;
                dy3 = y0-y1+y2-y3;

                float a11, a12, a13, a21, a22, a23, a31, a32;

                if (dx3 == 0 && dy3 == 0) {
                        a11 = x1-x0;
                        a21 = x2-x1;
                        a31 = x0;
                        a12 = y1-y0;
                        a22 = y2-y1;
                        a32 = y0;
                        a13 = a23 = 0;
                } else {
                        a13 = (dx3*dy2-dx2*dy3)/(dx1*dy2-dy1*dx2);
                        a23 = (dx1*dy3-dy1*dx3)/(dx1*dy2-dy1*dx2);
                        a11 = x1-x0+a13*x1;
                        a21 = x3-x0+a23*x3;
                        a31 = x0;
                        a12 = y1-y0+a13*y1;
                        a22 = y3-y0+a23*y3;
                        a32 = y0;
                }

            A = a22 - a32*a23;
            B = a31*a23 - a21;
            C = a21*a32 - a31*a22;
            D = a32*a13 - a12;
            E = a11 - a31*a13;
            F = a31*a12 - a11*a32;
            G = a12*a23 - a22*a13;
            H = a21*a13 - a11*a23;
            I = a11*a22 - a21*a12;


            return filter(src,dst);

        }



        protected void transformSpace(Rectangle rect) {
                rect.x = (int)Math.min( Math.min( x0, x1 ), Math.min( x2, x3 ) );
                rect.y = (int)Math.min( Math.min( y0, y1 ), Math.min( y2, y3 ) );
                rect.width = (int)Math.max( Math.max( x0, x1 ), Math.max( x2, x3 ) ) - rect.x;
                rect.height = (int)Math.max( Math.max( y0, y1 ), Math.max( y2, y3 ) ) - rect.y;
        }

    
        public float getOriginX() {
                return x0 - (int)Math.min( Math.min( x0, x1 ), Math.min( x2, x3 ) );
        }

        public float getOriginY() {
                return y0 - (int)Math.min( Math.min( y0, y1 ), Math.min( y2, y3 ) );
        }
    

        private BufferedImage filter( BufferedImage src, BufferedImage dst ) {
            int width = src.getWidth();
            int height = src.getHeight();
            int type = src.getType();
            WritableRaster srcRaster = src.getRaster();

            originalSpace = new Rectangle(0, 0, width, height);
            transformedSpace = new Rectangle(0, 0, width, height);
            transformSpace(transformedSpace);

            if ( dst == null ) {
                ColorModel dstCM = src.getColorModel();
                            dst = new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(transformedSpace.width, transformedSpace.height), dstCM.isAlphaPremultiplied(), null);
            }
            WritableRaster dstRaster = dst.getRaster();

            int[] inPixels = getRGB( src, 0, 0, width, height, null );

            if ( interpolation == NEAREST_NEIGHBOUR )
                    return filterPixelsNN( dst, width, height, inPixels, transformedSpace );

            int srcWidth = width;
            int srcHeight = height;
            int srcWidth1 = width-1;
            int srcHeight1 = height-1;
            int outWidth = transformedSpace.width;
            int outHeight = transformedSpace.height;
            int outX, outY;
            int index = 0;
            int[] outPixels = new int[outWidth];

            outX = transformedSpace.x;
            outY = transformedSpace.y;
            float[] out = new float[2];

            for (int y = 0; y < outHeight; y++) {
                for (int x = 0; x < outWidth; x++) {
                    transformInverse(outX+x, outY+y, out);
                    int srcX = (int)Math.floor( out[0] );
                    int srcY = (int)Math.floor( out[1] );
                    float xWeight = out[0]-srcX;
                    float yWeight = out[1]-srcY;
                    int nw, ne, sw, se;

                    if ( srcX >= 0 && srcX < srcWidth1 && srcY >= 0 && srcY < srcHeight1) {
                            // Easy case, all corners are in the image
                            int i = srcWidth*srcY + srcX;
                            nw = inPixels[i];
                            ne = inPixels[i+1];
                            sw = inPixels[i+srcWidth];
                            se = inPixels[i+srcWidth+1];
                    } else {
                            // Some of the corners are off the image
                            nw = getPixel( inPixels, srcX, srcY, srcWidth, srcHeight );
                            ne = getPixel( inPixels, srcX+1, srcY, srcWidth, srcHeight );
                            sw = getPixel( inPixels, srcX, srcY+1, srcWidth, srcHeight );
                            se = getPixel( inPixels, srcX+1, srcY+1, srcWidth, srcHeight );
                    }
                    outPixels[x] = bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se); 
                }
                setRGB( dst, 0, y, transformedSpace.width, 1, outPixels );
            }
                return dst;
        }

        final private int getPixel( int[] pixels, int x, int y, int width, int height ) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                switch (edgeAction) {
                case ZERO:
                default:
                    return 0;
                case WRAP:
                    return pixels[(mod(y, height) * width) + mod(x, width)];
                case CLAMP:
                    return pixels[(clamp(y, 0, height-1) * width) + clamp(x, 0, width-1)];
                }
            }
            return pixels[ y*width+x ];
        }


        protected BufferedImage filterPixelsNN( BufferedImage dst, int width, int height, int[] inPixels, Rectangle transformedSpace ) {
            int srcWidth = width;
            int srcHeight = height;
            int outWidth = transformedSpace.width;
            int outHeight = transformedSpace.height;
            int outX, outY, srcX, srcY;
            int[] outPixels = new int[outWidth];

            outX = transformedSpace.x;
            outY = transformedSpace.y;
            int[] rgb = new int[4];
            float[] out = new float[2];

            for (int y = 0; y < outHeight; y++) {
                for (int x = 0; x < outWidth; x++) {
                    transformInverse(outX+x, outY+y, out);
                    srcX = (int)out[0];
                    srcY = (int)out[1];
                    // int casting rounds towards zero, so we check out[0] < 0, not srcX < 0
                    if (out[0] < 0 || srcX >= srcWidth || out[1] < 0 || srcY >= srcHeight) {
                            int p;
                            switch (edgeAction) {
                            case ZERO:
                            default:
                                p = 0;
                                break;
                            case WRAP:
                                p = inPixels[(mod(srcY, srcHeight) * srcWidth) + mod(srcX, srcWidth)];
                                break;
                            case CLAMP:
                                p = inPixels[(clamp(srcY, 0, srcHeight-1) * srcWidth) + clamp(srcX, 0, srcWidth-1)];
                                break;
                            }
                            outPixels[x] = p;
                    } else {
                            int i = srcWidth*srcY + srcX;
                            rgb[0] = inPixels[i];
                            outPixels[x] = inPixels[i];
                    }
                }
                setRGB( dst, 0, y, transformedSpace.width, 1, outPixels );
            }
            return dst;
        }


        protected void transformInverse(int x, int y, float[] out) {
                out[0] = originalSpace.width * (A*x+B*y+C)/(G*x+H*y+I);
                out[1] = originalSpace.height * (D*x+E*y+F)/(G*x+H*y+I);
        }

/*
        public Rectangle2D getBounds2D( BufferedImage src ) {
            return new Rectangle(0, 0, src.getWidth(), src.getHeight());
        }

        public Point2D getPoint2D( Point2D srcPt, Point2D dstPt ) {
            if ( dstPt == null )
                dstPt = new Point2D.Double();
            dstPt.setLocation( srcPt.getX(), srcPt.getY() );
            return dstPt;
        }
*/
        
        /**
         * A convenience method for getting ARGB pixels from an image. This tries to avoid the performance
         * penalty of BufferedImage.getRGB unmanaging the image.
         */
        public int[] getRGB( BufferedImage image, int x, int y, int width, int height, int[] pixels ) {
                int type = image.getType();
                if ( type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB )
                        return (int [])image.getRaster().getDataElements( x, y, width, height, pixels );
                return image.getRGB( x, y, width, height, pixels, 0, width );
        }

        /**
         * A convenience method for setting ARGB pixels in an image. This tries to avoid the performance
         * penalty of BufferedImage.setRGB unmanaging the image.
         */
        public void setRGB( BufferedImage image, int x, int y, int width, int height, int[] pixels ) {
                int type = image.getType();
                if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB)
                    image.getRaster().setDataElements( x, y, width, height, pixels );
                else
                    image.setRGB( x, y, width, height, pixels, 0, width );
        }
        
        
        
	/**
	 * Clamp a value to an interval.
	 * @param a the lower clamp threshold
	 * @param b the upper clamp threshold
	 * @param x the input parameter
	 * @return the clamped value
	 */
	private float clamp(float x, float a, float b) {
		return (x < a) ? a : (x > b) ? b : x;
	}

	/**
	 * Clamp a value to an interval.
	 * @param a the lower clamp threshold
	 * @param b the upper clamp threshold
	 * @param x the input parameter
	 * @return the clamped value
	 */
	private int clamp(int x, int a, int b) {
		return (x < a) ? a : (x > b) ? b : x;
	}

	/**
	 * Return a mod b. This differs from the % operator with respect to negative numbers.
	 * @param a the dividend
	 * @param b the divisor
	 * @return a mod b
	 */
	private double mod(double a, double b) {
		int n = (int)(a/b);
		
		a -= n*b;
		if (a < 0)
			return a + b;
		return a;
	}

	/**
	 * Return a mod b. This differs from the % operator with respect to negative numbers.
	 * @param a the dividend
	 * @param b the divisor
	 * @return a mod b
	 */
	private float mod(float a, float b) {
		int n = (int)(a/b);
		
		a -= n*b;
		if (a < 0)
			return a + b;
		return a;
	}

	/**
	 * Return a mod b. This differs from the % operator with respect to negative numbers.
	 * @param a the dividend
	 * @param b the divisor
	 * @return a mod b
	 */
	private int mod(int a, int b) {
		int n = a/b;
		
		a -= n*b;
		if (a < 0)
			return a + b;
		return a;
	}
        
        
	/**
	 * Bilinear interpolation of ARGB values.
	 * @param x the X interpolation parameter 0..1
	 * @param y the y interpolation parameter 0..1
	 * @param rgb array of four ARGB values in the order NW, NE, SW, SE
	 * @return the interpolated value
	 */
	private int bilinearInterpolate(float x, float y, int nw, int ne, int sw, int se) {
		float m0, m1;
		int a0 = (nw >> 24) & 0xff;
		int r0 = (nw >> 16) & 0xff;
		int g0 = (nw >> 8) & 0xff;
		int b0 = nw & 0xff;
		int a1 = (ne >> 24) & 0xff;
		int r1 = (ne >> 16) & 0xff;
		int g1 = (ne >> 8) & 0xff;
		int b1 = ne & 0xff;
		int a2 = (sw >> 24) & 0xff;
		int r2 = (sw >> 16) & 0xff;
		int g2 = (sw >> 8) & 0xff;
		int b2 = sw & 0xff;
		int a3 = (se >> 24) & 0xff;
		int r3 = (se >> 16) & 0xff;
		int g3 = (se >> 8) & 0xff;
		int b3 = se & 0xff;

		float cx = 1.0f-x;
		float cy = 1.0f-y;

		m0 = cx * a0 + x * a1;
		m1 = cx * a2 + x * a3;
		int a = (int)(cy * m0 + y * m1);

		m0 = cx * r0 + x * r1;
		m1 = cx * r2 + x * r3;
		int r = (int)(cy * m0 + y * m1);

		m0 = cx * g0 + x * g1;
		m1 = cx * g2 + x * g3;
		int g = (int)(cy * m0 + y * m1);

		m0 = cx * b0 + x * b1;
		m1 = cx * b2 + x * b3;
		int b = (int)(cy * m0 + y * m1);

		return (a << 24) | (r << 16) | (g << 8) | b;
	}
        

    }

}
