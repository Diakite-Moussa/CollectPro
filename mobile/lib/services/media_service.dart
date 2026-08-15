import 'package:image_picker/image_picker.dart';
import 'package:file_picker/file_picker.dart';

class MediaService {
  final ImagePicker _picker = ImagePicker();

  /// Prends une photo avec l'appareil photo de l'appareil.
  Future<String?> takePhoto() async {
    try {
      final XFile? photo = await _picker.pickImage(
        source: ImageSource.camera,
        imageQuality: 80,
      );
      return photo?.path;
    } catch (_) {
      return null;
    }
  }

  /// Sélectionne une image depuis la galerie.
  Future<String?> pickImageFromGallery() async {
    try {
      final XFile? image = await _picker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 80,
      );
      return image?.path;
    } catch (_) {
      return null;
    }
  }

  /// Sélectionne un document (PDF, etc.) depuis l'appareil.
  Future<String?> pickDocument() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['pdf', 'doc', 'docx', 'txt', 'jpg', 'jpeg', 'png'],
      );
      if (result != null && result.files.isNotEmpty) {
        return result.files.single.path;
      }
      return null;
    } catch (_) {
      return null;
    }
  }
}
