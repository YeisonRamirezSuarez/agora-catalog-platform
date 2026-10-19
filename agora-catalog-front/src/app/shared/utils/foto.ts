/** Lo que acepta el backend por foto (ImagenService): 5 MB y 8000 px por lado. */
export const MAX_FOTO = 5 * 1024 * 1024;
const MAX_LADO = 4000;

/**
 * Una foto tomada con el celular suele pasar de 5 MB: en vez de rechazarla, se achica en el navegador
 * (lado mayor a 4000 px, JPG) antes de subirla. Las que ya cumplen salen tal cual.
 * Lanza un Error con el mensaje para el dueño si no se puede leer o no baja de 5 MB.
 */
export async function prepararFoto(archivo: File): Promise<File> {
  if (archivo.size <= MAX_FOTO) return archivo;
  let img: ImageBitmap;
  try {
    img = await createImageBitmap(archivo);
  } catch {
    throw new Error('No pudimos abrir la foto. Usa una JPG, PNG o WebP.');
  }
  const escala = Math.min(1, MAX_LADO / Math.max(img.width, img.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(img.width * escala);
  canvas.height = Math.round(img.height * escala);
  const ctx = canvas.getContext('2d')!;
  ctx.fillStyle = '#FFFFFF'; // un PNG transparente queda sobre blanco, como en el backend
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
  img.close();
  for (const calidad of [0.9, 0.82, 0.72]) {
    const blob = await new Promise<Blob | null>(listo => canvas.toBlob(listo, 'image/jpeg', calidad));
    if (blob && blob.size <= MAX_FOTO) {
      return new File([blob], archivo.name.replace(/\.[^.]+$/, '') + '.jpg', {type: 'image/jpeg'});
    }
  }
  throw new Error('La foto pesa demasiado. Elige una más liviana.');
}
