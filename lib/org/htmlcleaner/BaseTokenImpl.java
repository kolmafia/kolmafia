package org.htmlcleaner;

/**
 * Base class for all tokens. Allows position tracking.
 *
 * @author Konstantin Burov (aectann@gmail.com)
 *
 */
public abstract class BaseTokenImpl implements BaseToken {

    private int row;
    private int col;
    
    protected BaseTokenImpl(){
    	
    }

    protected BaseTokenImpl(int row, int col) {
        this.row = row;
        this.col = col;
    }
    public int getRow() {
        return row;
    }
    public void setRow(int row) {
        this.row = row;
    }
    public int getCol() {
        return col;
    }
    public void setCol(int col) {
        this.col = col;
    }

    @Override
    public String toString() {
        return "(line="+getRow()+", col="+getCol()+")";
    }

}
