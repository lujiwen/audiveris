//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t F o n t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.text.FontInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TextFont} is meant to simplify the use of text font for pieces of text
 * such as titles, directions, lyrics, etc.
 *
 * @author Hervé Bitteur
 */
public class TextFont
        extends OmrFont
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TextFont.class);

    /** Name of the chosen underlying text font */
    private static final String fontName = constants.defaultTextFontName.getValue();

    /** The base font used for text entities */
    public static final TextFont baseTextFont = new TextFont(
            constants.defaultTextFontSize.getValue());

    /** Ratio from a 300 DPI scan to font point-size (72 pt/inch) */
    public static final float TO_POINT = 72f / 300f;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TextFont object.
     *
     * @param fontName the font name. This can be a font face name or a font
     *                 family name, and may represent either a logical font or
     *                 a physical font found in this {@code GraphicsEnvironment}.
     * @param style    bit-mask style constant for the {@code Font}
     * @param size     the point size of the {@code Font}
     */
    public TextFont (String fontName,
                     int style,
                     int size)
    {
        super(fontName, style, size);
    }

    /**
     * Creates a font based on OCR font information.
     *
     * @param info OCR-based font information
     */
    public TextFont (FontInfo info)
    {
        this(
                info.isSerif ? Font.SERIF : (info.isMonospace ? Font.MONOSPACED : Font.SANS_SERIF),
                (info.isBold ? Font.BOLD : 0) | (info.isItalic ? Font.ITALIC : 0),
                info.pointsize);
    }

    /**
     * Creates a new TextFont object.
     *
     * @param size the point size of the {@code Font}
     */
    public TextFont (int size)
    {
        super(fontName, Font.PLAIN, size);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //-----------------//
    // computeFontSize //
    //-----------------//
    /**
     * Convenient method to compute a font size using a string content and dimension.
     *
     * @param content  the string value
     * @param fontInfo OCR-based font information
     * @param dim      string dimension in pixels
     * @return the computed font size
     */
    public static Float computeFontSize (String content,
                                         FontInfo fontInfo,
                                         Dimension dim)
    {
        if (content == null) {
            return null;
        }

        Font font = new TextFont(fontInfo);
        float fontSize = font.getSize2D();
        GlyphVector glyphVector = font.createGlyphVector(frc, content);
        Rectangle2D basicRect = glyphVector.getVisualBounds();

        if (dim.width >= dim.height) {
            return fontSize * (dim.width / (float) basicRect.getWidth());
        } else {
            return fontSize * (dim.height / (float) basicRect.getHeight());
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String defaultTextFontName = new Constant.String(
                "Serif",
                "Default font name for texts");

        private final Constant.Integer defaultTextFontSize = new Constant.Integer(
                "points",
                10,
                "Default font point size for texts");
    }
}
