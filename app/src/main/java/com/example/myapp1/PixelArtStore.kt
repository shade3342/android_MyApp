package com.example.myapp1

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

// 픽셀 그림 한 장의 데이터. pixels = ARGB 정수 배열(가로*세로), gridSize = 한 변의 칸 수,
//   file = 앱 내부에 저장된 원본 파일(없으면 = 아직 저장 안 한 새 그림).
data class PixelDoc(
    val pixels: IntArray,
    val gridSize: Int,
    val file: File?
) {
    // data class + IntArray 는 기본 equals/hashCode 가 참조 비교라 부정확 → 명시적으로 내용 비교.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PixelDoc) return false
        return gridSize == other.gridSize && file == other.file && pixels.contentEquals(other.pixels)
    }
    override fun hashCode(): Int {
        var result = pixels.contentHashCode()
        result = 31 * result + gridSize
        result = 31 * result + (file?.hashCode() ?: 0)
        return result
    }
}

// 픽셀 그림의 저장/불러오기/삭제/내보내기를 담당.
//   - '프로젝트'(재수정용) : 앱 내부 저장소(filesDir/pixelart)에 원본 해상도 PNG(무손실, 알파 보존).
//     원본 해상도라 다시 열면 픽셀·크기가 100% 그대로 복원된다.
//   - '내보내기'           : 휴대폰 공용 갤러리(Pictures/PixelArt)에 확대 PNG(투명 배경) 저장.
object PixelArtStore {

    private fun projectDir(context: Context): File =
        File(context.filesDir, "pixelart").apply { mkdirs() }

    // 저장된 프로젝트 파일 목록(최근 수정 순).
    fun list(context: Context): List<File> =
        projectDir(context)
            .listFiles { f -> f.isFile && f.name.endsWith(".png") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    // PNG 파일 → PixelDoc(픽셀 배열 + 크기). 원본 해상도라 그대로 읽으면 끝.
    fun load(file: File): PixelDoc {
        val bmp = BitmapFactory.decodeFile(file.absolutePath)
        val n = bmp.width // 정사각이라 가로=세로
        val px = IntArray(n * n)
        bmp.getPixels(px, 0, n, 0, 0, n, n)
        bmp.recycle()
        return PixelDoc(px, n, file)
    }

    // 프로젝트 저장. doc.file 이 있으면 그 파일을 덮어쓰고(=재수정 저장), 없으면 새 파일 생성.
    //   반환 = 저장된 파일(새 그림이면 새로 만든 파일).
    fun save(context: Context, doc: PixelDoc): File {
        val file = doc.file ?: File(projectDir(context), "proj_${System.currentTimeMillis()}.png")
        val bmp = Bitmap.createBitmap(doc.gridSize, doc.gridSize, Bitmap.Config.ARGB_8888)
        bmp.setPixels(doc.pixels, 0, doc.gridSize, 0, 0, doc.gridSize, doc.gridSize)
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }

    fun delete(file: File) {
        file.delete()
    }

    // ── 임시저장(draft) ── 작업 중인 '새 그림'을 앱 종료 후에도 이어 그릴 수 있게 보관.
    //   프로젝트 폴더가 아니라 filesDir 바로 아래에 두어 갤러리 목록(list)에는 안 잡히게 한다.
    private fun draftFile(context: Context): File = File(context.filesDir, "draft.png")

    fun saveDraft(context: Context, pixels: IntArray, gridSize: Int) {
        val bmp = Bitmap.createBitmap(gridSize, gridSize, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, gridSize, 0, 0, gridSize, gridSize)
        FileOutputStream(draftFile(context)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }

    // 임시저장 그림 로드. file=null 로 돌려줘서 편집기가 '아직 저장 안 한 새 그림'으로 다룬다.
    fun loadDraft(context: Context): PixelDoc? {
        val f = draftFile(context)
        if (!f.exists()) return null
        return load(f).copy(file = null)
    }

    fun clearDraft(context: Context) {
        draftFile(context).delete()
    }

    // 휴대폰 갤러리로 내보내기. 도트가 또렷하게 보이도록 정수배 확대(보간 끔), 투명 배경 유지.
    //   Android 10(Q)+ : MediaStore Pictures/PixelArt (권한 불필요, 갤러리 노출).
    //   9 이하         : 앱 전용 외부 저장소(권한 불필요, 갤러리 노출은 제한).
    //   반환 = 사람이 읽을 저장 위치.
    fun exportToGallery(context: Context, doc: PixelDoc): String {
        val small = Bitmap.createBitmap(doc.gridSize, doc.gridSize, Bitmap.Config.ARGB_8888)
        small.setPixels(doc.pixels, 0, doc.gridSize, 0, 0, doc.gridSize, doc.gridSize)
        val scale = (1024 / doc.gridSize).coerceAtLeast(1)
        val bitmap = Bitmap.createScaledBitmap(small, doc.gridSize * scale, doc.gridSize * scale, false)
        small.recycle()

        val filename = "pixelart_${System.currentTimeMillis()}.png"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixelArt")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore 항목 생성 실패")
            resolver.openOutputStream(uri).use { out ->
                requireNotNull(out) { "출력 스트림을 열 수 없음" }
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Pictures/PixelArt/$filename"
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(dir, filename)
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            file.absolutePath
        }
    }
}
