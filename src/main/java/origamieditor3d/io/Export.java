package origamieditor3d.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import origamieditor3d.graphics.Camera;
import origamieditor3d.origami.Geometry;
import origamieditor3d.origami.Origami;
import origamieditor3d.origami.OrigamiException;
import origamieditor3d.origami.OrigamiGen1;
import origamieditor3d.resources.Constants;
import origamieditor3d.resources.Instructor;

/**
 * A collection of methods for exporting origami into various formats.
 *
 * @author Attila Bágyoni (ba-sz-at@users.sourceforge.net)
 * @since 2013-01-14
 */
public class Export {

    final static public int page_width = 595;
    final static public int page_height = 842;
    final static public int figure_frame = 200;

    static private void writeIntLE(OutputStream out, int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        out.write(buffer.array());
    }

    static private void writeFloatLE(OutputStream out, float value) throws IOException {
        writeIntLE(out, Float.floatToIntBits(value));
    }

    static public void exportCTM(Origami origami, String filename, BufferedImage texture) throws Exception {

        try (DataOutputStream str = new DataOutputStream(Files.newOutputStream(Paths.get(filename),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING))) {

            Camera kamera = new Camera(0, 0, 1);
            kamera.adjust(origami);

            int haromszogek_hossz = 0;
            for (int i = 0; i < origami.getPolygonsSize(); i++) {

                if (origami.isNonDegenerate(i)) {
                    haromszogek_hossz += origami.getPolygons().get(i).size() - 2;
                }
            }

            //OCTM
            writeIntLE(str, 0x4d54434f);

            //5. verzió
            writeIntLE(str, 0x00000005);

            //RAW tömörítés
            writeIntLE(str, 0x00574152);

            //Pontok száma
            writeIntLE(str, origami.getVerticesSize());

            //Háromszögek száma
            writeIntLE(str, haromszogek_hossz);

            //UV térképek száma
            writeIntLE(str, texture == null ? 0 : 1);

            //Attibrútumtérképek száma
            writeIntLE(str, 0x00000000);

            //Csúcsonkénti merôlegesek nincsenek
            writeIntLE(str, 0x00000000);

            //Reklám
            byte[] comment = "Created with Origami Editor 3D. ".getBytes();
            writeIntLE(str, comment.length);
            str.write(comment);

            //INDX
            writeIntLE(str, 0x58444e49);

            //Háromszögek
            for (int i = 0; i < origami.getPolygonsSize(); i++) {
                if (origami.isNonDegenerate(i)) {
                    for (int ii = 1; ii < origami.getPolygons().get(i).size() - 1; ii++) {
                        writeIntLE(str, origami.getPolygons().get(i).get(0));
                        writeIntLE(str, origami.getPolygons().get(i).get(ii));
                        writeIntLE(str, origami.getPolygons().get(i).get(ii + 1));
                    }
                }
            }

            //VERT
            writeIntLE(str, 0x54524556);

            //Csúcsok
            for (int i = 0; i < origami.getVerticesSize(); i++) {
                for (int j = 0; j < 3; j++) {
                    writeFloatLE(str, (float) ((float)origami.getVertices().get(i)[j] - (float)kamera.getCamPosition()[j]));
                }
            }

            if (texture != null) {
                writeIntLE(str, 0x43584554);

                writeIntLE(str, 0x00000005);
                str.write("Paper".getBytes());

                long u = 0;
                File teximg = new File(filename + "-texture.png");

                while (teximg.exists()) {

                    teximg = new File(filename + "-texture" + u + ".png");
                    u++;
                }

                ImageIO.write(texture, "png", teximg);

                byte[] teximgname = teximg.getName().getBytes();
                writeIntLE(str, teximgname.length);
                str.write(teximgname);

                //the UV mapping is defined by the vertices in the paper space
                for (int i = 0; i < origami.getVerticesSize(); i++) {
                    writeFloatLE(str, (float) (origami.getVertices2d().get(i)[0] / origami.paperWidth()));
                    writeFloatLE(str, (float) (1 - origami.getVertices2d().get(i)[1] / origami.paperHeight()));
                }
            }

            System.out.println(str.size() + " bytes written to " + filename);
            kamera.unadjust(origami);

        } catch (IOException exc) {
            throw OrigamiException.H005;
        }
    }

    static public void exportPDF(Origami origami, String filename, String title) throws Exception {

        try (DataOutputStream str = new DataOutputStream(Files.newOutputStream(Paths.get(filename),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING))) {

            Origami origami1 = origami.copy();

            //Itt tároljuk az objektumok offszeteit
            ArrayList<Integer> Offszetek = new ArrayList<>();
            Offszetek.add(0);
            int bajtszam = 0;

            //Megszámoljuk, hány mûvelet nem lesz külön feltüntetve
            ArrayList<Integer> UresIndexek = new ArrayList<>();

            for (int i = 0; i < origami1.getHistory().size(); i++) {
                if (origami1.getHistory().get(i).foldID == Origami.FoldingAction.FOLD_CREASE) {
                    UresIndexek.add(i);
                } else if (i < origami1.getHistory().size() - 1 && origami1.getHistory().get(i).foldID == origami1.getHistory().get(i+1).foldID) {
                    if (origami1.getHistory().get(i).foldID == Origami.FoldingAction.FOLD_ROTATION) {
                        if (origami1.getHistory().get(i + 1).ppoint == origami1.getHistory().get(i).ppoint &&
                                origami1.getHistory().get(i + 1).pnormal == origami1.getHistory().get(i).pnormal) {
                            UresIndexek.add(i + 1);
                        }
                    } else if (origami1.getHistory().get(i).foldID == Origami.FoldingAction.FOLD_ROTATION_P) {
                        if (origami1.getHistory().get(i + 1).ppoint == origami1.getHistory().get(i).ppoint
                                && origami1.getHistory().get(i + 1).pnormal == origami1.getHistory().get(i).pnormal
                                && origami1.getHistory().get(i + 1).polygonIndex == origami1.getHistory().get(i).polygonIndex) {
                            UresIndexek.add(i + 1);
                        }
                    }
                }
            }

            //Azok a lépések, amikhez szemszögváltás kell
            ArrayList<Integer> ForgatasIndexek = new ArrayList<>();
            //A szemszögváltások függôleges forgásszögei
            ArrayList<Integer> ForgatasSzogek = new ArrayList<>();

            ArrayList<Integer> foldtypes = new ArrayList<>();
            boolean firstblood = true;

            //Méretezés és elôigazítás
            Camera kamera = new Camera(0, 0, 0.5);
            kamera.nextOrthogonalView();

            //Felmérjük az olyan lépések számát, amikhez szemszögváltás kell.
            for (int i = 0; i < origami1.getHistory().size(); i++) {

                double[] regiVaszonNV = kamera.getCamDirection();

                kamera.setCamDirection(Geometry.crossProduct(origami1.getHistory().get(i).pnormal,
                        new double[]{0, 1, 0}));

                if (Geometry.vectorLength(kamera.getCamDirection()) < .00000001) {
                    kamera.setCamDirection(new double[]{0, 0, 1});
                }

                kamera.setCamDirection(Geometry.normalizeVector(kamera.getCamDirection()));

                if (Geometry.vectorLength(Geometry.crossProduct(regiVaszonNV, kamera.getCamDirection())) > .00000001) {

                    ForgatasIndexek.add(i);
                    double cos = Geometry.scalarProduct(regiVaszonNV, kamera.getCamDirection()) / Geometry.vectorLength(regiVaszonNV) / Geometry.vectorLength(kamera.getCamDirection());
                    ForgatasSzogek.add((int) (Math.acos(cos >= -1 && cos <= 1 ? cos : 1) / Math.PI * 180));
                }
            }
            ForgatasIndexek.add(origami1.getHistory().size());

            //Egy oldalon 6 cella van (papírmérettôl függetlenül)
            int cellak_szama = origami1.getHistory().size() + ForgatasIndexek.size() - UresIndexek.size() + 2;

            //Fejléc
            String fajl = "";
            fajl += "%PDF-1.3\n\n";

            //Katalógus
            Offszetek.add(fajl.length());
            fajl += "1 0 obj\n";
            fajl += "<< /Type /Catalog\n";
            fajl += " /Pages 2 0 R\n";
            fajl += ">>\n";
            fajl += "endobj\n\n";

            //Kötet
            Offszetek.add(fajl.length());
            fajl += "2 0 obj\n";
            fajl += "<< /Type /Pages\n";
            fajl += "/Kids [";
            fajl += "3 0 R";

            //Az oldalak száma a cellák számának hatoda felfelé kerekítve
            for (int i = 1; i < (int) Math.ceil((double) cellak_szama / 6); i++) {

                fajl += " " + Integer.toString(i + 3) + " 0 R";
            }
            fajl += "]\n";
            fajl += "/Count " + Integer.toString((int) Math.ceil((double) cellak_szama / 6)) + "\n";
            fajl += "/MediaBox [0 0 " + Integer.toString(page_width) + " " + Integer.toString(page_height) + "]\n";
            fajl += ">>\n";
            fajl += "endobj\n\n";

            //Oldalak
            for (int i = 0; i < (int) Math.ceil((double) cellak_szama / 6); i++) {

                Offszetek.add(fajl.length());
                fajl += "" + Integer.toString(i + 3) + " 0 obj\n";
                fajl += "<< /Type /Page\n";
                fajl += "/Parent 2 0 R\n";
                fajl += "/Resources\n";
                fajl += "<< /Font\n";
                fajl += "<< /F1\n";
                fajl += "<< /Type /Font\n";
                fajl += "/Subtype /Type1\n";
                fajl += "/BaseFont /Courier\n";
                fajl += ">>\n";
                fajl += ">>\n";
                fajl += ">>\n";
                fajl += "/Contents[";

                //Egy oldalon általánosan 6 kép és 6 szöveg objektum van
                //A fájltest elsô felében a képek, a másodikban a szövegek vannak
                for (int ii = (int) Math.ceil((double) cellak_szama / 6) + i * 6;
                        ii < (cellak_szama < (i + 1) * 6
                                ? (int) Math.ceil((double) cellak_szama / 6) + cellak_szama
                                : (int) Math.ceil((double) cellak_szama / 6) + (i + 1) * 6);
                        ii++) {
                    if (ii != (int) Math.ceil((double) cellak_szama / 6) + i * 6) {
                        fajl += " ";
                    }
                    fajl += Integer.toString(ii + 3) + " 0 R";
                    fajl += " " + Integer.toString(ii + cellak_szama + 3) + " 0 R";
                }
                fajl += "]\n";
                fajl += ">>\n";
                fajl += "endobj\n\n";
            }

            //A cím a megadott fájlnév
            Offszetek.add(fajl.length());
            String stream;
            stream = "BT\n";
            stream += "/F1 18 Tf\n";
            stream += "100 800 Td\n";
            stream += "(";
            for (int i = 0; i < 18 - title.length() / 2; i++) {
                stream += " ";
            }
            stream += title + ") Tj\n";
            stream += "ET\n";
            fajl += Integer.toString((int) Math.ceil((double) cellak_szama / 6) + 3) + " 0 obj\n";
            fajl += "<< /Length " + Integer.toString(stream.length()) + " >>\n";
            fajl += "stream\n";
            fajl += stream;
            fajl += "endstream\n";
            fajl += "endobj\n\n";

            //A cím alatti két üres cellában van helyünk a reklámozásra
            Offszetek.add(fajl.length());
            stream = "BT\n";
            stream += "/F1 12 Tf\n";
            stream += Integer.toString((int) (page_width - 2 * figure_frame) / 4) + " 760 Td\n";
            stream += "14 TL\n";
            stream += Instructor.getString("disclaimer", Constants.VERSION) + "\n";
            stream += "ET\n";
            fajl += Integer.toString((int) Math.ceil((double) cellak_szama / 6) + 4) + " 0 obj\n";
            fajl += "<< /Length " + Integer.toString(stream.length()) + " >>\n";
            fajl += "stream\n";
            fajl += stream;
            fajl += "endstream\n";
            fajl += "endobj\n\n";
            str.writeBytes(fajl);
            bajtszam += fajl.length();
            fajl = "";

            //Ez már élesben megy
            origami1.reset();
            double maxdim = origami1.circumscribedSquareSize();
            if (maxdim == .0) {
                maxdim = 1.;
            }

            kamera = new Camera(0, 0, figure_frame / maxdim);
            kamera.nextOrthogonalView();
            kamera.unadjust(origami1);

            //Az objektum indexe, ahol épp tartunk
            int objindex = (int) Math.ceil((double) cellak_szama / 6) + 5;



            //Ábrák
            for (int i = 0; i <= origami1.getHistory().size(); i++) {

                int x = 0, y = 0;
                String kep;

                if (ForgatasIndexek.contains(i)) {

                    int position = (objindex - (int) Math.ceil((double) cellak_szama / 6) - 3) % 6;
                    x = page_width / 4 * (2*(position%2)+1);
                    y = (page_height / 3 - figure_frame) / 4 + page_height / 6 * (5 - 2*(position/2));

                    kamera.adjust(origami1);
                    kamera.setZoom(figure_frame / Math.max(kamera.circumscribedSquareSize(origami1), 1.) * kamera.getZoom());
                    kep = kamera.drawFaces(x, y, origami1) + kamera.drawEdges(x, y, origami1);

                    Offszetek.add(bajtszam);
                    stream = "q";
                    stream += " ";
                    stream += kep;
                    stream += "Q\n";
                    fajl += Integer.toString(objindex) + " 0 obj\n";
                    fajl += "<< /Length " + Integer.toString(stream.length()) + " >>\n";
                    fajl += "stream\n";
                    fajl += stream;
                    fajl += "endstream\n";
                    fajl += "endobj\n\n";
                    objindex++;
                    str.writeBytes(fajl);
                    bajtszam += fajl.length();
                    fajl = "";
                }

                if (!UresIndexek.contains(i) && i < origami1.getHistory().size()) {

                    double[] regiVaszonNV = kamera.getCamDirection();

                    kamera.setCamDirection(Geometry.crossProduct(origami1.getHistory().get(i).pnormal,
                            new double[]{0, 1, 0}));

                    if (Geometry.scalarProduct(kamera.getCamDirection(), kamera.getCamDirection()) < 0.00000001) {
                        kamera.setCamDirection(new double[]{0, 0, 1});
                    }

                    kamera.setCamDirection(new double[]{kamera.getCamDirection()[0] / Geometry.vectorLength(kamera.getCamDirection()),
                        kamera.getCamDirection()[1] / Geometry.vectorLength(kamera.getCamDirection()),
                        kamera.getCamDirection()[2] / Geometry.vectorLength(kamera.getCamDirection())});

                    kamera.setYAxis(new double[]{0, 1, 0});
                    kamera.setXAxis(Geometry.crossProduct(kamera.getCamDirection(), kamera.getYAxis()));

                    kamera.setXAxis(new double[]{kamera.getXAxis()[0] / Geometry.vectorLength(kamera.getXAxis()) * kamera.getZoom(),
                        kamera.getXAxis()[1] / Geometry.vectorLength(kamera.getXAxis()) * kamera.getZoom(),
                        kamera.getXAxis()[2] / Geometry.vectorLength(kamera.getXAxis()) * kamera.getZoom()});

                    kamera.setYAxis(new double[]{kamera.getYAxis()[0] / Geometry.vectorLength(kamera.getYAxis()) * kamera.getZoom(),
                        kamera.getYAxis()[1] / Geometry.vectorLength(kamera.getYAxis()) * kamera.getZoom(),
                        kamera.getYAxis()[2] / Geometry.vectorLength(kamera.getYAxis()) * kamera.getZoom()});

                    if (Geometry.scalarProduct(regiVaszonNV, kamera.getCamDirection()) < 0 && !ForgatasIndexek.contains(i)) {

                        kamera.setCamDirection(Geometry.vectorDiff(Geometry.nullvector, kamera.getCamDirection()));
                        kamera.setXAxis(Geometry.vectorDiff(Geometry.nullvector, kamera.getXAxis()));
                    }

                    int position = (objindex - (int) Math.ceil((double) cellak_szama / 6) - 3) % 6;
                    x = page_width / 4 * (2*(position%2)+1);
                    y = (page_height / 3 - figure_frame) / 4 + page_height / 6 * (5 - 2*(position/2));

                    double[] sikpont;
                    double[] siknv;

                    kamera.adjust(origami1);
                    kamera.setZoom(figure_frame / Math.max(kamera.circumscribedSquareSize(origami1), 1.) * kamera.getZoom());

                    switch (origami1.getHistory().get(i).foldID) {

                        case Origami.FoldingAction.FOLD_REFLECTION:
                        case Origami.FoldingAction.FOLD_ROTATION:
                        case Origami.FoldingAction.FOLD_CREASE:
                        case Origami.FoldingAction.FOLD_MUTILATION:
                            sikpont = origami1.getHistory().get(i).ppoint;
                            siknv = origami1.getHistory().get(i).pnormal;
                            kep = kamera.drawFaces(x, y, origami1) + kamera.drawEdges(x, y, origami1) + kamera.pfdLiner(x, y, sikpont, siknv);
                            break;

                        case Origami.FoldingAction.FOLD_REFLECTION_P:
                        case Origami.FoldingAction.FOLD_ROTATION_P:
                        case Origami.FoldingAction.FOLD_MUTILATION_P:
                            sikpont = origami1.getHistory().get(i).ppoint;
                            siknv = origami1.getHistory().get(i).pnormal;
                            kep = kamera.drawSelection(x, y, sikpont, siknv, origami1.getHistory().get(i).polygonIndex, origami1) + kamera.drawEdges(x, y, origami1) + kamera.pfdLiner(x, y, sikpont, siknv);
                            break;

                        default:
                            kep = kamera.drawFaces(x, y, origami1) + kamera.drawEdges(x, y, origami1);
                            break;
                    }

                    Offszetek.add(bajtszam);
                    stream = "q";
                    stream += " ";
                    stream += kep;
                    stream += "Q\n";
                    fajl += Integer.toString(objindex) + " 0 obj\n";
                    fajl += "<< /Length " + Integer.toString(stream.length()) + " >>\n";
                    fajl += "stream\n";
                    fajl += stream;
                    fajl += "endstream\n";
                    fajl += "endobj\n\n";
                    objindex++;
                    str.writeBytes(fajl);
                    bajtszam += fajl.length();
                    fajl = "";
                }
                origami1.execute(i, 1);
                if (i < origami1.getHistory().size()) {
                    if (origami1.getHistory().get(i).foldID == 1) {

                        double[] ppoint = origami1.getHistory().get(i).ppoint;
                        double[] pnormal = origami1.getHistory().get(i).pnormal;
                        foldtypes.add(origami1.foldType(ppoint, pnormal));
                    } else if (origami1.getHistory().get(i).foldID == 3) {

                        double[] ppoint = origami1.getHistory().get(i).ppoint;
                        double[] pnormal = origami1.getHistory().get(i).pnormal;
                        int polygonIndex = origami1.getHistory().get(i).polygonIndex;
                        foldtypes.add(origami1.foldType(ppoint, pnormal, polygonIndex));
                    } else {
                        foldtypes.add(null);
                    }
                }
            }

            int dif = OrigamiGen1.difficultyLevel(origami1.difficulty());
            String difname = Instructor.getString("level" + dif);
            Offszetek.add(bajtszam);
            stream = "BT\n";
            stream += "/F1 12 Tf\n";
            stream += Integer.toString((int) (page_width - 2 * figure_frame) / 4) + " "
                    + Integer.toString(722 - Instructor.getString("disclaimer", Constants.VERSION).length() * 14
                            + Instructor.getString("disclaimer", Constants.VERSION).replace(") '", ") ").length() * 14)
                    + " Td\n";
            stream += "12 TL\n";
            stream += Instructor.getString("difficulty", dif, difname) + "\n";
            stream += "ET\n";
            fajl += Integer.toString(objindex) + " 0 obj\n";
            fajl += "<< /Length " + Integer.toString(stream.length()) + " >>\n";
            fajl += "stream\n";
            fajl += stream;
            fajl += "endstream\n";
            fajl += "endobj\n\n";
            objindex++;
            str.writeBytes(fajl);
            bajtszam += fajl.length();
            fajl = "";

            Offszetek.add(bajtszam);
            stream = "BT\n";
            stream += "/F1 12 Tf\n";
            stream += Integer.toString((int) (page_width - 2 * figure_frame) / 4) + " "
                    + Integer.toString(736 - Instructor.getString("disclaimer", Constants.VERSION).length() * 14
                            + Instructor.getString("disclaimer", Constants.VERSION).replace(") '", ") ").length() * 14)
                    + " Td\n";
            stream += Instructor.getString("steps", cellak_szama - 2) + "Tj\n";
            stream += "ET\n";
            fajl += Integer.toString(objindex) + " 0 obj\n";
            fajl += "<< /Length " + Integer.toString(stream.length()) + " >>\n";
            fajl += "stream\n";
            fajl += stream;
            fajl += "endstream\n";
            fajl += "endobj\n\n";
            objindex++;
            str.writeBytes(fajl);
            bajtszam += fajl.length();
            fajl = "";

            int sorszam = 1;

            //Szövegek
            for (int i = 0; i <= origami1.getHistory().size(); i++) {

                String utasitas = "";
                String koo = "";

                double[] siknv;

                if (ForgatasIndexek.contains(i)) {

                    if (i == origami1.getHistory().size()) {

                        utasitas = Instructor.getString("outro", sorszam);
                        sorszam++;
                    } else {

                        utasitas = Instructor.getString("turn", sorszam, ForgatasSzogek.get(ForgatasIndexek.indexOf(i)));
                        sorszam++;
                    }

                    int position = sorszam % 6;
                    koo = (page_width / 2 * (position % 2) + (page_width / 2 - figure_frame) / 3) + " ";
                    koo += Integer.toString(page_height / 3 * (2-position/2) + (page_height / 3 - figure_frame) / 2 + (page_height / 3 - figure_frame) / 4);

                    Offszetek.add(bajtszam);
                    stream = "BT\n";
                    stream += "/F1 10 Tf\n";
                    stream += koo + " Td\n";
                    stream += "12 TL\n";
                    stream += utasitas + "\n";
                    stream += "ET\n";
                    fajl += Integer.toString(objindex + sorszam - 2) + " 0 obj\n";
                    fajl += "<< /Length " + Integer.toString(stream.length()) + " >>\n";
                    fajl += "stream\n";
                    fajl += stream;
                    fajl += "endstream\n";
                    fajl += "endobj\n\n";
                    str.writeBytes(fajl);
                    bajtszam += fajl.length();
                    fajl = "";
                }

                if (!UresIndexek.contains(i) && i < origami1.getHistory().size()) {

                    double[] regiVaszonNV = kamera.getCamDirection();

                    kamera.setCamDirection(Geometry.crossProduct(origami1.getHistory().get(i).pnormal,
                            new double[]{0, 1, 0}));

                    if (Geometry.scalarProduct(kamera.getCamDirection(), kamera.getCamDirection()) < 0.00000001) {
                        kamera.setCamDirection(new double[]{0, 0, 1});
                    }

                    kamera.setCamDirection(new double[]{kamera.getCamDirection()[0] / Geometry.vectorLength(kamera.getCamDirection()),
                        kamera.getCamDirection()[1] / Geometry.vectorLength(kamera.getCamDirection()),
                        kamera.getCamDirection()[2] / Geometry.vectorLength(kamera.getCamDirection())});

                    kamera.setYAxis(new double[]{0, 1, 0});
                    kamera.setXAxis(Geometry.crossProduct(kamera.getCamDirection(), kamera.getYAxis()));

                    kamera.setXAxis(new double[]{kamera.getXAxis()[0] / Geometry.vectorLength(kamera.getXAxis()) * kamera.getZoom(),
                        kamera.getXAxis()[1] / Geometry.vectorLength(kamera.getXAxis()) * kamera.getZoom(),
                        kamera.getXAxis()[2] / Geometry.vectorLength(kamera.getXAxis()) * kamera.getZoom()});

                    kamera.setYAxis(new double[]{kamera.getYAxis()[0] / Geometry.vectorLength(kamera.getYAxis()) * kamera.getZoom(),
                        kamera.getYAxis()[1] / Geometry.vectorLength(kamera.getYAxis()) * kamera.getZoom(),
                        kamera.getYAxis()[2] / Geometry.vectorLength(kamera.getYAxis()) * kamera.getZoom()});

                    if (Geometry.scalarProduct(regiVaszonNV, kamera.getCamDirection()) < 0 && !ForgatasIndexek.contains(i)) {

                        kamera.setCamDirection(Geometry.vectorDiff(Geometry.nullvector, kamera.getCamDirection()));
                        kamera.setXAxis(Geometry.vectorDiff(Geometry.nullvector, kamera.getXAxis()));
                    }

                    switch (origami1.getHistory().get(i).foldID) {

                        case Origami.FoldingAction.FOLD_REFLECTION:
                            siknv = origami1.getHistory().get(i).pnormal;
                            switch (foldtypes.get(i)) {

                                case 0:
                                    utasitas = Instructor.getString("no_fold", sorszam);
                                    break;

                                case -1:
                                    utasitas = Instructor.getString("fold_" + kamera.pdfLinerDir(siknv), sorszam);
                                    break;

                                case -2:
                                    utasitas = Instructor.getString("fold/rev_" + kamera.pdfLinerDir(siknv), sorszam);
                                    break;

                                case -3:
                                    utasitas = Instructor.getString("rev_" + kamera.pdfLinerDir(siknv), sorszam);
                                    break;

                                case -4:
                                    utasitas = Instructor.getString("fold/sink_" + kamera.pdfLinerDir(siknv), sorszam);
                                    break;

                                case -5:
                                    utasitas = Instructor.getString("rev/sink_" + kamera.pdfLinerDir(siknv), sorszam);
                                    break;

                                default:
                                    utasitas = Instructor.getString("compound", sorszam, foldtypes.get(i));
                                    break;
                            }

                            sorszam++;
                            break;

                        case Origami.FoldingAction.FOLD_ROTATION:
                            siknv = origami1.getHistory().get(i).pnormal;
                            int szog = 0;
                            int j = i - 1;
                            while (UresIndexek.contains(j)) {

                                if (origami1.getHistory().get(j).foldID == Origami.FoldingAction.FOLD_ROTATION) {

                                    szog += origami1.getHistory().get(j).phi;
                                }
                                j--;
                            }

                            utasitas = Instructor.getString("rotate_" + kamera.pdfLinerDir(siknv), sorszam, szog + origami1.getHistory().get(i).phi);
                            sorszam++;
                            break;

                        case Origami.FoldingAction.FOLD_REFLECTION_P:
                            switch (foldtypes.get(i)) {

                                case 0:
                                    utasitas = Instructor.getString("no_fold", sorszam);
                                    break;
                                case -1:
                                    utasitas = Instructor.getString("fold_gray", sorszam);
                                    break;
                                case -2:
                                    utasitas = Instructor.getString("fold/rev_gray", sorszam);
                                    break;
                                case -3:
                                    utasitas = Instructor.getString("rev_gray", sorszam);
                                    break;
                                case -4:
                                    utasitas = Instructor.getString("fold/sink_gray", sorszam);
                                    break;
                                case -5:
                                    utasitas = Instructor.getString("rev/sink_gray", sorszam);
                                    break;
                            }
                            sorszam++;
                            break;

                        case Origami.FoldingAction.FOLD_ROTATION_P:
                            int szog1 = 0;
                            int j1 = i - 1;
                            while (UresIndexek.contains(j1)) {

                                if (origami1.getHistory().get(j1).foldID == Origami.FoldingAction.FOLD_ROTATION_P) {

                                    szog1 += origami1.getHistory().get(j1).phi;
                                }
                                j1--;
                            }
                            utasitas = Instructor.getString("rotate_gray", sorszam, szog1 + origami1.getHistory().get(i).phi);
                            sorszam++;
                            break;

                        case Origami.FoldingAction.FOLD_CREASE:
                            utasitas = Instructor.getString("crease", sorszam, sorszam + 1);
                            sorszam++;
                            break;

                        case Origami.FoldingAction.FOLD_MUTILATION:
                            siknv = origami1.getHistory().get(i).pnormal;
                            utasitas = Instructor.getString("cut_" + kamera.pdfLinerDir(siknv), sorszam);
                            if (firstblood) {
                                utasitas += Instructor.getString("cut_notice");
                                firstblood = false;
                            }
                            sorszam++;
                            break;

                        case Origami.FoldingAction.FOLD_MUTILATION_P:
                            utasitas = Instructor.getString("cut_gray", sorszam);
                            if (firstblood) {
                                utasitas += Instructor.getString("cut_notice");
                                firstblood = false;
                            }
                            sorszam++;
                            break;

                        default:
                            utasitas = "(" + sorszam + ". ???) ' ";
                            sorszam++;
                            break;
                    }

                    if (i == 0) {

                        switch (origami1.getPaperType()) {

                            case A4:
                                utasitas = Instructor.getString("intro_a4", sorszam) + utasitas;
                                break;
                            case Square:
                                utasitas = Instructor.getString("intro_square", sorszam) + utasitas;
                                break;
                            case Hexagon:
                                utasitas = Instructor.getString("intro_hex", sorszam) + utasitas;
                                break;
                            case Dollar:
                                utasitas = Instructor.getString("intro_dollar", sorszam) + utasitas;
                                break;
                            case Custom:
                                if (origami1.getCorners().size() == 3) {
                                    utasitas = Instructor.getString("intro_triangle", sorszam) + utasitas;
                                } else if (origami1.getCorners().size() == 4) {
                                    utasitas = Instructor.getString("intro_quad", sorszam) + utasitas;
                                } else {
                                    utasitas = Instructor.getString("intro_poly", sorszam) + utasitas;
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    int position = sorszam % 6;
                    koo = (page_width / 2 * (position % 2) + (page_width / 2 - figure_frame) / 3) + " ";
                    koo += Integer.toString(page_height / 3 * (2-position/2) + (page_height / 3 - figure_frame) / 2 + (page_height / 3 - figure_frame) / 4);

                    Offszetek.add(bajtszam);
                    stream = "BT\n";
                    stream += "/F1 10 Tf\n";
                    stream += koo + " Td\n";
                    stream += "12 TL\n";
                    stream += utasitas + "\n";
                    stream += "ET\n";
                    fajl += Integer.toString(objindex + sorszam - 2) + " 0 obj\n";
                    fajl += "<< /Length " + Integer.toString(stream.length()) + " >>\n";
                    fajl += "stream\n";
                    fajl += stream;
                    fajl += "endstream\n";
                    fajl += "endobj\n\n";
                    str.writeBytes(fajl);
                    bajtszam += fajl.length();
                    fajl = "";
                }
            }

            int xroffszet = bajtszam;

            fajl += "xref\n";
            fajl += "0 " + Integer.toString(Offszetek.size()) + "\n";
            fajl += "0000000000 65535 f \n";

            for (int i = 1; i < Offszetek.size(); i++) {

                for (int ii = 0; ii < 10 - Integer.toString(Offszetek.get(i)).length(); ii++) {
                    fajl += "0";
                }
                fajl += Integer.toString(Offszetek.get(i));
                fajl += " 00000 n \n";
            }

            fajl += "trailer\n";
            fajl += "<< /Root 1 0 R\n";
            fajl += "/Size " + Integer.toString(Offszetek.size()) + "\n";
            fajl += ">>\n";
            fajl += "startxref\n";
            fajl += Integer.toString(xroffszet) + "\n";
            fajl += "%%EOF";

            str.writeBytes(fajl);
            System.out.println(str.size() + " bytes written to " + filename);

        } catch (Exception exc) {
            throw OrigamiException.H005;
        }
    }

    static public void exportGIF(Origami origami, Camera refcam, int color, int width, int height, String filename) throws Exception {

        try (FileOutputStream fos = new FileOutputStream(filename)) {

            fos.write('G');
            fos.write('I');
            fos.write('F');
            fos.write('8');
            fos.write('9');
            fos.write('a');

            fos.write((byte) width);
            fos.write((byte) (width >>> 8));
            fos.write((byte) height);
            fos.write((byte) (height >>> 8));
            fos.write(0b10010110);
            fos.write(0);
            fos.write(0);

            for (int r = 1; r <= 5; r++) {
                for (int g = 1; g <= 5; g++) {
                    for (int b = 1; b <= 5; b++) {

                        fos.write(r * 51);
                        fos.write(g * 51);
                        fos.write(b * 51);
                    }
                }
            }
            for (int i = 0; i < 9; i++) {
                fos.write(0);
            }

            fos.write(0x21);
            fos.write(0xFF);
            fos.write(0x0B);
            fos.write('N');
            fos.write('E');
            fos.write('T');
            fos.write('S');
            fos.write('C');
            fos.write('A');
            fos.write('P');
            fos.write('E');
            fos.write('2');
            fos.write('.');
            fos.write('0');
            fos.write(0x03);
            fos.write(0x01);
            fos.write(0x00);
            fos.write(0x00);
            fos.write(0x00);

            BufferedImage img = new BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D gimg = img.createGraphics();
            gimg.setBackground(java.awt.Color.WHITE);
            Origami origami1 = origami.copy();
            Camera cam = new Camera(width / 2, height / 2, 1);
            cam.setCamDirection(refcam.getCamDirection().clone());
            cam.setXAxis(refcam.getXAxis().clone());
            cam.setYAxis(refcam.getYAxis().clone());
            cam.setZoom(0.8 * Math.min(width, height) / origami1.circumscribedSquareSize());
            int steps = origami1.getHistoryPointer();
            origami1.undo(steps);
            boolean last = false;
            while (origami1.getHistoryPointer() < steps || (last = !last && origami1.getHistoryPointer() == steps)) {

                gimg.clearRect(0, 0, width, height);
                cam.adjust(origami1);
                cam.drawFaces(gimg, color, origami1);
                cam.drawEdges(gimg, java.awt.Color.black, origami1);
                origami1.redo();
                fos.write(0x21);
                fos.write(0xF9);
                fos.write(0x04);
                fos.write(0x04);
                fos.write(0x64); //delay time
                fos.write(0x00);
                fos.write(0x00);
                fos.write(0x00);

                fos.write(0x2C);
                fos.write(0x00);
                fos.write(0x00);
                fos.write(0x00);
                fos.write(0x00);
                fos.write((byte) width);
                fos.write((byte) (width >>> 8));
                fos.write((byte) height);
                fos.write((byte) (height >>> 8));
                fos.write(0x00);

                fos.write(0x07);

                for (int y = 0; y < height; y++) {

                    fos.write(width / 2 + 1);
                    fos.write(0x80);
                    for (int x = 0; x < width / 2; x++) {

                        int rgb = img.getRGB(x, y) & 0xFFFFFF;
                        int b = rgb % 0x100;
                        int g = (rgb >>> 8) % 0x100;
                        int r = rgb >>> 16;

                        fos.write((((r * 5) / 256) * 25 + ((g * 5) / 256) * 5 + (b * 5) / 256));
                    }
                    fos.write(width - width / 2 + 1);
                    fos.write(0x80);
                    for (int x = width / 2; x < width; x++) {

                        int rgb = img.getRGB(x, y) & 0xFFFFFF;
                        int b = rgb % 0x100;
                        int g = (rgb >>> 8) % 0x100;
                        int r = rgb >>> 16;

                        fos.write((((r * 5) / 256) * 25 + ((g * 5) / 256) * 5 + (b * 5) / 256));
                    }
                }
                fos.write(0x01);
                fos.write(0x81);
                fos.write(0);
            }

            fos.write(0x3B);
            System.out.println(fos.getChannel().position() + " bytes written to " + filename);
            fos.close();

        } catch (IOException ex) {
            throw OrigamiException.H005;
        }
    }

    static public void exportRevolvingGIF(Origami origami, Camera refcam, int color, int width, int height, String filename) throws Exception {

        try (FileOutputStream fos = new FileOutputStream(filename)) {

            fos.write('G');
            fos.write('I');
            fos.write('F');
            fos.write('8');
            fos.write('9');
            fos.write('a');

            fos.write((byte) width);
            fos.write((byte) (width >>> 8));
            fos.write((byte) height);
            fos.write((byte) (height >>> 8));
            fos.write(0b10010110);
            fos.write(0);
            fos.write(0);

            for (int r = 1; r <= 5; r++) {
                for (int g = 1; g <= 5; g++) {
                    for (int b = 1; b <= 5; b++) {

                        fos.write(r * 51);
                        fos.write(g * 51);
                        fos.write(b * 51);
                    }
                }
            }
            for (int i = 0; i < 9; i++) {
                fos.write(0);
            }

            fos.write(0x21);
            fos.write(0xFF);
            fos.write(0x0B);
            fos.write('N');
            fos.write('E');
            fos.write('T');
            fos.write('S');
            fos.write('C');
            fos.write('A');
            fos.write('P');
            fos.write('E');
            fos.write('2');
            fos.write('.');
            fos.write('0');
            fos.write(0x03);
            fos.write(0x01);
            fos.write(0x00);
            fos.write(0x00);
            fos.write(0x00);

            BufferedImage img = new BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D gimg = img.createGraphics();
            gimg.setBackground(java.awt.Color.WHITE);
            Origami origami1 = origami.copy();
            Camera cam = new Camera(width / 2, height / 2, 1);
            cam.setCamDirection(refcam.getCamDirection().clone());
            cam.setXAxis(refcam.getXAxis().clone());
            cam.setYAxis(refcam.getYAxis().clone());
            cam.setZoom(0.8 * Math.min(width, height) / origami1.circumscribedSquareSize());
            cam.adjust(origami1);

            for (int i = 0; i < 72; i++) {

                gimg.clearRect(0, 0, width, height);
                cam.drawGradient(gimg, color, origami1);
                cam.drawEdges(gimg, java.awt.Color.black, origami1);
                cam.rotate(10, 0);

                fos.write(0x21);
                fos.write(0xF9);
                fos.write(0x04);
                fos.write(0x04);
                fos.write(0x05); //delay time
                fos.write(0x00);
                fos.write(0x00);
                fos.write(0x00);

                fos.write(0x2C);
                fos.write(0x00);
                fos.write(0x00);
                fos.write(0x00);
                fos.write(0x00);
                fos.write((byte) width);
                fos.write((byte) (width >>> 8));
                fos.write((byte) height);
                fos.write((byte) (height >>> 8));
                fos.write(0x00);

                fos.write(0x07);

                for (int y = 0; y < height; y++) {

                    fos.write(width / 2 + 1);
                    fos.write(0x80);
                    for (int x = 0; x < width / 2; x++) {

                        int rgb = img.getRGB(x, y) & 0xFFFFFF;
                        int b = rgb % 0x100;
                        int g = (rgb >>> 8) % 0x100;
                        int r = rgb >>> 16;

                        fos.write((((r * 5) / 256) * 25 + ((g * 5) / 256) * 5 + (b * 5) / 256));
                    }
                    fos.write(width - width / 2 + 1);
                    fos.write(0x80);
                    for (int x = width / 2; x < width; x++) {

                        int rgb = img.getRGB(x, y) & 0xFFFFFF;
                        int b = rgb % 0x100;
                        int g = (rgb >>> 8) % 0x100;
                        int r = rgb >>> 16;

                        fos.write((((r * 5) / 256) * 25 + ((g * 5) / 256) * 5 + (b * 5) / 256));
                    }
                }
                fos.write(0x01);
                fos.write(0x81);
                fos.write(0);
            }

            fos.write(0x3B);
            System.out.println(fos.getChannel().position() + " bytes written to " + filename);
            fos.close();

        } catch (IOException ex) {
            throw OrigamiException.H005;
        }
    }

    static public void exportPNG(Origami origami, String filename) throws Exception {

        try {

            File png = new File(filename);
            if (png.exists()) {
                png.delete();
            }
            BufferedImage img = new BufferedImage((int) origami.paperWidth(), (int) origami.paperHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setBackground(java.awt.Color.WHITE);
            g.clearRect(0, 0, (int) origami.paperWidth(), (int) origami.paperHeight());
            new Camera((int) origami.paperWidth() / 2, (int) origami.paperHeight() / 2, 1).drawCreasePattern(g, Color.BLACK, origami);

            if (!ImageIO.write(img, "png", png)) {
                throw OrigamiException.H005;
            }
        } catch (Exception ex) {
            throw OrigamiException.H005;
        }
    }

    static public void exportJAR(Origami origami, String filename, int[] rgb) throws Exception {

        try {

            File finalJar = new File(filename);
            if (finalJar.exists()) {
                finalJar.delete();
            }
            long ordinal = 1;
            File tempJar;
            while ((tempJar = new File(finalJar.getParentFile(), ordinal + ".jar")).exists()
                    || tempJar.equals(finalJar)) {
                ordinal++;
            }
            ordinal = 1;
            File tempOri;
            while ((tempOri = new File(finalJar.getParentFile(), ordinal + ".ori")).exists()
                    || tempOri.equals(finalJar)) {
                ordinal++;
            }

            InputStream is = new Export().getClass().getResourceAsStream("/res/OrigamiDisplay.jar");
            OutputStream os = new FileOutputStream(tempJar);

            int nextbyte;
            while ((nextbyte = is.read()) != -1) {
                os.write(nextbyte);
            }

            is.close();
            os.close();

            OrigamiIO.write_gen2(origami, tempOri.getPath(), rgb);

            ZipFile jar = new ZipFile(tempJar);
            FileOutputStream fos = new FileOutputStream(finalJar);
            ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry next;

            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                if (!(next = entries.nextElement()).isDirectory()) {

                    zos.putNextEntry(next);
                    is = jar.getInputStream(next);

                    while ((nextbyte = is.read()) != -1) {
                        zos.write(nextbyte);
                    }
                    zos.closeEntry();
                    is.close();
                }
            }

            next = new ZipEntry("o");
            zos.putNextEntry(next);
            is = new FileInputStream(tempOri);
            while ((nextbyte = is.read()) != -1) {
                zos.write(nextbyte);
            }

            zos.closeEntry();

            System.out.println(fos.getChannel().position() + " bytes written to " + filename);
            zos.close();
            fos.close();
            is.close();
            jar.close();

            System.out.print("Cleaning up... ");
            tempOri.delete();
            tempJar.delete();
            System.out.println("done");

        } catch (Exception ex) {
            throw OrigamiException.H005;
        }
    }
}
