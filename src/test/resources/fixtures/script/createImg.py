# @OpService ops
# @float[] arr
# @long[] dims
# @OUTPUT Img out

from net.imagej.ops import Ops
from net.imglib2.img import Img
from net.imglib2.img.array import ArrayImgs

arr = list(arr)
dims = list(dims)
out = ArrayImgs.floats(arr, dims)
