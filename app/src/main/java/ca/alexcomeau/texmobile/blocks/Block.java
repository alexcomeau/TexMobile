package ca.alexcomeau.texmobile.blocks;

import java.io.Serializable;

import ca.alexcomeau.texmobile.Coordinate;

public abstract class Block implements Serializable {
    protected int rotation;
    protected Coordinate position;
    protected int blockColor;
    // Nested arrays should be based on a 4x4 grid. Order doesn't really matter.
    protected Coordinate[][] rotations;

    public Block(Coordinate start, Coordinate[][] rotate, int color)
    {
        rotation = 0;
        position = start;
        blockColor = color;
        rotations = rotate;
    }

    public void moveUp() { position.y++; }
    public void moveDown() { position.y--; }
    public void moveLeft() { position.x--; }
    public void moveRight() { position.x++; }

    // Loops around the rotations clockwise
    public void rotateRight() { if(rotation++ == rotations.length) rotation = 0; }

    // Loops around the rotations counterclockwise
    public void rotateLeft() { if(rotation-- == -1) rotation = rotations.length - 1; }

    // Adds the position to the current relative coordinates and returns that.
    public Coordinate[] getAbsoluteCoordinates()
    {
        Coordinate[] coords = getRelativeCoordinates();
        for(Coordinate c : coords) {
            c = c.add(position);
        }
        return coords;
    }

    public int getBlockColor() { return blockColor; }
    public Coordinate getPosition() { return position; }
    public Coordinate[] getRelativeCoordinates() { return rotations[rotation]; }
}
