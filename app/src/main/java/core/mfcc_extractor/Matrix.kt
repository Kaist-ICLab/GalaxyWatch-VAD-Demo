package core.mfcc_extractor

class Matrix(private val rows: Int, private val cols: Int) {
    private val data: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }

    fun get(row: Int, col: Int): Double {
        return data[row][col]
    }

    fun set(row: Int, col: Int, value: Double) {
        data[row][col] = value
    }

    fun transpose(): Matrix {
        val transposed = Matrix(cols, rows)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                transposed.set(j, i, get(i, j))
            }
        }
        return transposed
    }

    fun toArray(): Array<DoubleArray> {
        return data
    }
}
