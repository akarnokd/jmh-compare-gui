
package hu.akarnokd.jmh.gui;

import java.awt.Image;
import java.awt.datatransfer.*;
import java.io.IOException;

public class TransferableImage implements Transferable {

    Image i;

    public TransferableImage( Image i ) {
        this.i = i;
    }

    @Override
    public Object getTransferData( DataFlavor flavor )
            throws UnsupportedFlavorException, IOException {
        if ( flavor.equals( DataFlavor.imageFlavor ) && i != null ) {
            return i;
        }
        else {
            throw new UnsupportedFlavorException( flavor );
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        DataFlavor[] flavors = new DataFlavor[ 1 ];
        flavors[ 0 ] = DataFlavor.imageFlavor;
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        DataFlavor[] flavors = getTransferDataFlavors();
        for ( int i = 0; i < flavors.length; i++ ) {
            if ( flavor.equals( flavors[ i ] ) ) {
                return true;
            }
        }

        return false;
    }
}