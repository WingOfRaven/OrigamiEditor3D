package origamieditor3d.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import origamieditor3d.graphics.Camera;
import origamieditor3d.origami.*;
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

        try (OutputStream os = Files.newOutputStream(Paths.get(filename),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
             DataOutputStream str = new DataOutputStream(os)) {

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

    private static void setCameraDirection(Camera kamera, Origami origami1, int i, boolean ignoreSign) {
        double[] regiVaszonNV = kamera.getCamDirection();

        kamera.setCamDirection(Geometry.crossProduct(origami1.getHistory().get(i).getPnormal(),
                new double[]{0, 1, 0}));

        if (Geometry.scalarProduct(kamera.getCamDirection(), kamera.getCamDirection()) < 0.00000001) {
            kamera.setCamDirection(new double[]{0, 0, 1});
        }

        kamera.setCamDirection(Geometry.normalizeVector(kamera.getCamDirection()));

        kamera.setYAxis(new double[]{0, 1, 0});
        kamera.setXAxis(Geometry.crossProduct(kamera.getCamDirection(), kamera.getYAxis()));

        kamera.setXAxis(Geometry.scalarMultiple(Geometry.normalizeVector(kamera.getXAxis()), kamera.getZoom()));

        kamera.setYAxis(Geometry.scalarMultiple(Geometry.normalizeVector(kamera.getYAxis()), kamera.getZoom()));

        if (Geometry.scalarProduct(regiVaszonNV, kamera.getCamDirection()) < 0 && !ignoreSign) {

            kamera.setCamDirection(Geometry.vectorDiff(Geometry.nullvector, kamera.getCamDirection()));
            kamera.setXAxis(Geometry.vectorDiff(Geometry.nullvector, kamera.getXAxis()));
        }
    }

    static private void appendObjStreamPDF(StringBuilder builder, int objIndex, String stream) {
        builder.append(objIndex).append(" 0 obj\n");
        builder.append("<< /Length ").append(stream.length()).append(" >>\n");
        builder.append("stream\n");
        builder.append(stream);
        builder.append("endstream\n");
        builder.append("endobj\n\n");
    }

    static public void exportPDF(Origami origami, String filename, String title) throws Exception {

        try (OutputStream os = Files.newOutputStream(Paths.get(filename),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
             DataOutputStream str = new DataOutputStream(os)) {

            Origami origami1 = origami.copy();

            //Itt tároljuk az objektumok offszeteit
            ArrayList<Integer> Offszetek = new ArrayList<>();
            Offszetek.add(0);
            int bajtszam = 0;

            //Megszámoljuk, hány mûvelet nem lesz külön feltüntetve
            ArrayList<Integer> UresIndexek = new ArrayList<>();

            for (int i = 0; i < origami1.getHistory().size(); i++) {
                if (origami1.getHistory().get(i) instanceof CommandFoldCrease) {
                    UresIndexek.add(i);
                } else if (i < origami1.getHistory().size() - 1 && origami1.getHistory().get(i).getClass().equals(origami1.getHistory().get(i+1).getClass())) {
                    if (origami1.getHistory().get(i) instanceof CommandFoldRotation) {
                        if (origami1.getHistory().get(i + 1).getPpoint() == origami1.getHistory().get(i).getPpoint() &&
                                origami1.getHistory().get(i + 1).getPnormal() == origami1.getHistory().get(i).getPnormal()) {
                            UresIndexek.add(i + 1);
                        }
                    } else if (origami1.getHistory().get(i) instanceof CommandFoldRotationP) {
                        if (origami1.getHistory().get(i + 1).getPpoint() == origami1.getHistory().get(i).getPpoint()
                                && origami1.getHistory().get(i + 1).getPnormal() == origami1.getHistory().get(i).getPnormal()
                                && origami1.getHistory().get(i + 1).getPolygonIndex() == origami1.getHistory().get(i).getPolygonIndex()) {
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

                kamera.setCamDirection(Geometry.crossProduct(origami1.getHistory().get(i).getPnormal(),
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
            StringBuilder fajl = new StringBuilder();
            fajl.append("%PDF-1.3\n\n");

            //Katalógus
            Offszetek.add(fajl.length());
            fajl.append("1 0 obj\n");
            fajl.append("<< /Type /Catalog\n");
            fajl.append(" /Pages 2 0 R\n");
            fajl.append(">>\n");
            fajl.append("endobj\n\n");

            //Kötet
            Offszetek.add(fajl.length());
            fajl.append("2 0 obj\n");
            fajl.append("<< /Type /Pages\n");
            fajl.append("/Kids [");
            fajl.append("3 0 R");

            //Az oldalak száma a cellák számának hatoda felfelé kerekítve
            for (int i = 1; i < (int) Math.ceil((double) cellak_szama / 6); i++) {

                fajl.append(" ").append(i + 3).append(" 0 R");
            }
            fajl.append("]\n");
            fajl.append("/Count ").append((int) Math.ceil((double) cellak_szama / 6)).append("\n");
            fajl.append("/MediaBox [0 0 ").append(page_width).append(" ").append(page_height).append("]\n");
            fajl.append(">>\n");
            fajl.append("endobj\n\n");

            //Oldalak
            for (int i = 0; i < (int) Math.ceil((double) cellak_szama / 6); i++) {

                Offszetek.add(fajl.length());
                fajl.append(i + 3).append(" 0 obj\n");
                fajl.append("<< /Type /Page\n");
                fajl.append("/Parent 2 0 R\n");
                fajl.append("/Resources\n");
                fajl.append("<< /Font\n");
                fajl.append("<< /F1\n");
                fajl.append("<< /Type /Font\n");
                fajl.append("/Subtype /Type1\n");
                fajl.append("/BaseFont /Courier\n");
                fajl.append(">>\n");
                fajl.append(">>\n");
                fajl.append(">>\n");
                fajl.append("/Contents[");

                //Egy oldalon általánosan 6 kép és 6 szöveg objektum van
                //A fájltest elsô felében a képek, a másodikban a szövegek vannak
                for (int ii = (int) Math.ceil((double) cellak_szama / 6) + i * 6;
                        ii < (cellak_szama < (i + 1) * 6
                                ? (int) Math.ceil((double) cellak_szama / 6) + cellak_szama
                                : (int) Math.ceil((double) cellak_szama / 6) + (i + 1) * 6);
                        ii++) {
                    if (ii != (int) Math.ceil((double) cellak_szama / 6) + i * 6) {
                        fajl.append(" ");
                    }
                    fajl.append(ii + 3).append(" 0 R");
                    fajl.append(" ").append(ii + cellak_szama + 3).append(" 0 R");
                }
                fajl.append("]\n");
                fajl.append(">>\n");
                fajl.append("endobj\n\n");
            }

            //A cím a megadott fájlnév
            Offszetek.add(fajl.length());
            StringBuilder stream;
            stream = new StringBuilder("BT\n");
            stream.append("/F1 18 Tf\n");
            stream.append("100 800 Td\n");
            stream.append("(");
            for (int i = 0; i < 18 - title.length() / 2; i++) {
                stream.append(" ");
            }
            stream.append(title).append(") Tj\n");
            stream.append("ET\n");
            appendObjStreamPDF(fajl, (int) Math.ceil((double) cellak_szama / 6) + 3, stream.toString());

            //A cím alatti két üres cellában van helyünk a reklámozásra
            Offszetek.add(fajl.length());
            stream = new StringBuilder("BT\n");
            stream.append("/F1 12 Tf\n");
            stream.append((int) (page_width - 2 * figure_frame) / 4).append(" 760 Td\n");
            stream.append("14 TL\n");
            stream.append(Instructor.getString("disclaimer", Constants.VERSION)).append("\n");
            stream.append("ET\n");
            appendObjStreamPDF(fajl, (int) Math.ceil((double) cellak_szama / 6) + 4, stream.toString());
            str.writeBytes(fajl.toString());
            bajtszam += fajl.length();
            fajl = new StringBuilder();

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

                if (ForgatasIndexek.contains(i) || (!UresIndexek.contains(i) && i < origami1.getHistory().size())) {

                    String kep;

                    int position = (objindex - (int) Math.ceil((double) cellak_szama / 6) - 3) % 6;
                    int x = page_width / 4 * (2*(position%2)+1);
                    int y = (page_height / 3 - figure_frame) / 4 + page_height / 6 * (5 - 2*(position/2));

                    if (ForgatasIndexek.contains(i)) {
                        kamera.adjust(origami1);
                        kamera.setZoom(figure_frame / Math.max(kamera.circumscribedSquareSize(origami1), 1.) * kamera.getZoom());
                        kep = kamera.drawFaces(x, y, origami1) + kamera.drawEdges(x, y, origami1);
                    } else {
                        setCameraDirection(kamera, origami1, i, ForgatasIndexek.contains(i));
                        kamera.adjust(origami1);
                        kamera.setZoom(figure_frame / Math.max(kamera.circumscribedSquareSize(origami1), 1.) * kamera.getZoom());

                        double[] sikpont;
                        double[] siknv;

                        CommandFold commandFold = origami1.getHistory().get(i);
                        if (commandFold instanceof CommandFoldReflection
                                || commandFold instanceof CommandFoldRotation
                                || commandFold instanceof CommandFoldCrease
                                || commandFold instanceof CommandFoldMutilation) {
                            sikpont = origami1.getHistory().get(i).getPpoint();
                            siknv = origami1.getHistory().get(i).getPnormal();
                            kep = kamera.drawFaces(x, y, origami1) + kamera.drawEdges(x, y, origami1) + kamera.pfdLiner(x, y, sikpont, siknv);
                        } else if (commandFold instanceof CommandFoldReflectionP
                                || commandFold instanceof CommandFoldRotationP
                                || commandFold instanceof CommandFoldMutilationP) {
                            sikpont = origami1.getHistory().get(i).getPpoint();
                            siknv = origami1.getHistory().get(i).getPnormal();
                            kep = kamera.drawSelection(x, y, sikpont, siknv, origami1.getHistory().get(i).getPolygonIndex(), origami1) + kamera.drawEdges(x, y, origami1) + kamera.pfdLiner(x, y, sikpont, siknv);
                        } else {
                            kep = kamera.drawFaces(x, y, origami1) + kamera.drawEdges(x, y, origami1);
                        }
                    }

                    Offszetek.add(bajtszam);
                    stream = new StringBuilder("q");
                    stream.append(" ");
                    stream.append(kep);
                    stream.append("Q\n");
                    appendObjStreamPDF(fajl, objindex, stream.toString());
                    objindex++;
                    str.writeBytes(fajl.toString());
                    bajtszam += fajl.length();
                    fajl = new StringBuilder();
                }
                origami1.execute(i, 1);
                if (i < origami1.getHistory().size()) {
                    if (origami1.getHistory().get(i) instanceof CommandFoldReflection) {

                        double[] ppoint = origami1.getHistory().get(i).getPpoint();
                        double[] pnormal = origami1.getHistory().get(i).getPnormal();
                        foldtypes.add(origami1.foldType(ppoint, pnormal));
                    } else if (origami1.getHistory().get(i) instanceof CommandFoldReflectionP) {

                        double[] ppoint = origami1.getHistory().get(i).getPpoint();
                        double[] pnormal = origami1.getHistory().get(i).getPnormal();
                        int polygonIndex = origami1.getHistory().get(i).getPolygonIndex();
                        foldtypes.add(origami1.foldType(ppoint, pnormal, polygonIndex));
                    } else {
                        foldtypes.add(null);
                    }
                }
            }

            int dif = OrigamiGen1.difficultyLevel(origami1.difficulty());
            String difname = Instructor.getString("level" + dif);
            Offszetek.add(bajtszam);
            stream = new StringBuilder("BT\n");
            stream.append("/F1 12 Tf\n");
            stream.append((int) (page_width - 2 * figure_frame) / 4).append(" ").append(722 - Instructor.getString("disclaimer", Constants.VERSION).length() * 14
                    + Instructor.getString("disclaimer", Constants.VERSION).replace(") '", ") ").length() * 14).append(" Td\n");
            stream.append("12 TL\n");
            stream.append(Instructor.getString("difficulty", dif, difname)).append("\n");
            stream.append("ET\n");
            appendObjStreamPDF(fajl, objindex, stream.toString());
            objindex++;
            str.writeBytes(fajl.toString());
            bajtszam += fajl.length();
            fajl = new StringBuilder();

            Offszetek.add(bajtszam);
            stream = new StringBuilder("BT\n");
            stream.append("/F1 12 Tf\n");
            stream.append((int) (page_width - 2 * figure_frame) / 4).append(" ").append(736 - Instructor.getString("disclaimer", Constants.VERSION).length() * 14
                    + Instructor.getString("disclaimer", Constants.VERSION).replace(") '", ") ").length() * 14).append(" Td\n");
            stream.append(Instructor.getString("steps", cellak_szama - 2)).append("Tj\n");
            stream.append("ET\n");
            appendObjStreamPDF(fajl, objindex, stream.toString());
            objindex++;
            str.writeBytes(fajl.toString());
            bajtszam += fajl.length();
            fajl = new StringBuilder();

            int sorszam = 1;

            //Szövegek
            for (int i = 0; i <= origami1.getHistory().size(); i++) {

                String utasitas = "";
                String koo = "";

                if (ForgatasIndexek.contains(i) || (!UresIndexek.contains(i) && i < origami1.getHistory().size())) {

                    if (ForgatasIndexek.contains(i)) {

                        if (i == origami1.getHistory().size()) {

                            utasitas = Instructor.getString("outro", sorszam);
                            sorszam++;
                        } else {

                            utasitas = Instructor.getString("turn", sorszam, ForgatasSzogek.get(ForgatasIndexek.indexOf(i)));
                            sorszam++;
                        }
                    } else {

                        setCameraDirection(kamera, origami1, i, ForgatasIndexek.contains(i));

                        String direction = null;

                        CommandFold commandFold = origami1.getHistory().get(i);

                        if (commandFold instanceof CommandFoldReflection
                                || commandFold instanceof CommandFoldRotation
                                || commandFold instanceof CommandFoldMutilation) {
                            double[] siknv = commandFold.getPnormal();
                            direction = kamera.pdfLinerDir(siknv).toString();
                        }
                        else if (commandFold instanceof CommandFoldReflectionP
                                || commandFold instanceof CommandFoldRotationP
                                || commandFold instanceof CommandFoldMutilationP) {
                            direction = "gray";
                        }

                        if (commandFold instanceof CommandFoldReflection || commandFold instanceof CommandFoldReflectionP) {
                            switch (foldtypes.get(i)) {

                                case 0:
                                    utasitas = Instructor.getString("no_fold", sorszam);
                                    break;

                                case -1:
                                    utasitas = Instructor.getString("fold_" + direction, sorszam);
                                    break;

                                case -2:
                                    utasitas = Instructor.getString("fold/rev_" + direction, sorszam);
                                    break;

                                case -3:
                                    utasitas = Instructor.getString("rev_" + direction, sorszam);
                                    break;

                                case -4:
                                    utasitas = Instructor.getString("fold/sink_" + direction, sorszam);
                                    break;

                                case -5:
                                    utasitas = Instructor.getString("rev/sink_" + direction, sorszam);
                                    break;

                                default:
                                    if (!direction.equals("gray"))
                                        utasitas = Instructor.getString("compound", sorszam, foldtypes.get(i));
                                    break;
                            }

                            sorszam++;
                        } else if (commandFold instanceof CommandFoldRotation || commandFold instanceof CommandFoldRotationP) {
                            int szog = 0;
                            int j = i - 1;
                            while (UresIndexek.contains(j)) {

                                if (origami1.getHistory().get(j).getClass().equals(origami1.getHistory().get(i).getClass())) {

                                    szog += origami1.getHistory().get(j).getPhi();
                                }
                                j--;
                            }

                            utasitas = Instructor.getString("rotate_" + direction, sorszam, szog + origami1.getHistory().get(i).getPhi());
                            sorszam++;
                        } else if (commandFold instanceof CommandFoldCrease) {
                            utasitas = Instructor.getString("crease", sorszam, sorszam + 1);
                            sorszam++;
                        } else if (commandFold instanceof CommandFoldMutilation || commandFold instanceof CommandFoldMutilationP) {
                            utasitas = Instructor.getString("cut_" + direction, sorszam);
                            if (firstblood) {
                                utasitas += Instructor.getString("cut_notice");
                                firstblood = false;
                            }
                            sorszam++;
                        } else {
                            utasitas = "(" + sorszam + ". ???) ' ";
                            sorszam++;
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
                    }

                    int position = sorszam % 6;
                    koo = (page_width / 2 * (position % 2) + (page_width / 2 - figure_frame) / 3) + " ";
                    koo += Integer.toString(page_height / 3 * (2-position/2) + (page_height / 3 - figure_frame) / 2 + (page_height / 3 - figure_frame) / 4);

                    Offszetek.add(bajtszam);
                    stream = new StringBuilder("BT\n");
                    stream.append("/F1 10 Tf\n");
                    stream.append(koo).append(" Td\n");
                    stream.append("12 TL\n");
                    stream.append(utasitas).append("\n");
                    stream.append("ET\n");
                    appendObjStreamPDF(fajl, objindex + sorszam - 2, stream.toString());
                    str.writeBytes(fajl.toString());
                    bajtszam += fajl.length();
                    fajl = new StringBuilder();
                }
            }

            int xroffszet = bajtszam;

            fajl.append("xref\n");
            fajl.append("0 ").append(Offszetek.size()).append("\n");
            fajl.append("0000000000 65535 f \n");

            for (int i = 1; i < Offszetek.size(); i++) {
                fajl.append(String.format("%010d", Offszetek.get(i)));
                fajl.append(" 00000 n \n");
            }

            fajl.append("trailer\n");
            fajl.append("<< /Root 1 0 R\n");
            fajl.append("/Size ").append(Offszetek.size()).append("\n");
            fajl.append(">>\n");
            fajl.append("startxref\n");
            fajl.append(xroffszet).append("\n");
            fajl.append("%%EOF");

            str.writeBytes(fajl.toString());
            System.out.println(str.size() + " bytes written to " + filename);

        } catch (Exception exc) {
            throw OrigamiException.H005;
        }
    }

    static private void exportGIF(Origami origami, Camera refcam, int color, int width, int height, String filename, boolean revolving) throws Exception {

        try (OutputStream os = Files.newOutputStream(Paths.get(filename),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
             DataOutputStream fos = new DataOutputStream(os)) {

            fos.write("GIF89a".getBytes());

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
            fos.write("NETSCAPE2.0".getBytes());
            fos.write(0x03);
            fos.write(0x01);
            fos.write(0x00);
            fos.write(0x00);
            fos.write(0x00);

            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D gimg = img.createGraphics();
            gimg.setBackground(Color.white);
            Origami origami1 = origami.copy();
            Camera cam = new Camera(width / 2, height / 2, 1);
            cam.setCamDirection(refcam.getCamDirection().clone());
            cam.setXAxis(refcam.getXAxis().clone());
            cam.setYAxis(refcam.getYAxis().clone());
            cam.setZoom(0.8 * Math.min(width, height) / origami1.circumscribedSquareSize());

            int steps;
            boolean last = false;

            if (!revolving) {
                steps = origami1.getHistoryPointer();
                origami1.undo(steps);
            } else {
                cam.adjust(origami1);
                steps = 0;
            }

            while ((!revolving && (origami1.getHistoryPointer() < steps || (last = !last && origami1.getHistoryPointer() == steps))) ||
                    (revolving && steps < 72)) {

                gimg.clearRect(0, 0, width, height);

                if (!revolving) {
                    cam.adjust(origami1);
                    cam.drawFaces(gimg, color, origami1);
                    cam.drawEdges(gimg, Color.black, origami1);
                    origami1.redo();
                } else {
                    cam.drawGradient(gimg, color, origami1);
                    cam.drawEdges(gimg, Color.black, origami1);
                    cam.rotate(10, 0);
                    steps++;
                }

                fos.write(0x21);
                fos.write(0xF9);
                fos.write(0x04);
                fos.write(0x04);
                fos.write(!revolving ? 0x64 : 0x05); //delay time
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
                    for (int x = 0; x < width; x++) {

                        if (x == width / 2) {
                            fos.write(width - width / 2 + 1);
                            fos.write(0x80);
                        }

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
            System.out.println(fos.size() + " bytes written to " + filename);

        } catch (IOException ex) {
            throw OrigamiException.H005;
        }
    }

    static public void exportGIF(Origami origami, Camera refcam, int color, int width, int height, String filename) throws Exception {
        exportGIF(origami, refcam, color, width, height, filename, false);
    }

    static public void exportRevolvingGIF(Origami origami, Camera refcam, int color, int width, int height, String filename) throws Exception {
        exportGIF(origami, refcam, color, width, height, filename, true);
    }

    static public void exportPNG(Origami origami, String filename) throws Exception {

        try (OutputStream png = Files.newOutputStream(Paths.get(filename),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            BufferedImage img = new BufferedImage((int) origami.paperWidth(), (int) origami.paperHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setBackground(Color.white);
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

        Path finalJarPath = Paths.get(filename);

        try (OutputStream os = Files.newOutputStream(finalJarPath,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
             DataOutputStream dos = new DataOutputStream(os);
             ZipOutputStream zos = new ZipOutputStream(dos)) {

            long ordinal = 1;
            Path tempOriPath;
            while (Files.exists(tempOriPath = finalJarPath.resolveSibling(ordinal + ".ori"))
                    || tempOriPath.equals(finalJarPath)) {
                ordinal++;
            }

            OrigamiIO.write_gen2(origami, tempOriPath.toString(), rgb);

            ZipInputStream jar = new ZipInputStream(Export.class.getResourceAsStream("/res/OrigamiDisplay.jar"));

            ZipEntry next;

            while ((next = jar.getNextEntry()) != null) {
                if (!next.isDirectory()) {
                    zos.putNextEntry(next);
                    jar.transferTo(zos);
                    zos.closeEntry();
                }
            }

            next = new ZipEntry("o");
            zos.putNextEntry(next);
            Files.copy(tempOriPath, zos);
            zos.closeEntry();

            System.out.println(dos.size() + " bytes written to " + filename);
            System.out.print("Cleaning up... ");
            Files.delete(tempOriPath);
            System.out.println("done");

        } catch (Exception ex) {
            throw OrigamiException.H005;
        }
    }
}
