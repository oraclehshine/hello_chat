import 'dart:convert';

String normalizeDisplayText(String? input) {
  if (input == null || input.isEmpty) return '';
  var text = input.replaceAll(
    RegExp(r'[\u0000-\u0008\u000B\u000C\u000E-\u001F]'),
    '',
  );
  text = text.replaceAll(String.fromCharCode(0xfffd), '');
  if (_looksLikeMisdecodedUtf8(text)) {
    text = _tryRecoverLatin1Utf8(text);
  }
  return text;
}

bool _looksLikeMisdecodedUtf8(String value) {
  return value.contains('Ã') || value.contains('Â') || value.contains('ð');
}

String _tryRecoverLatin1Utf8(String value) {
  try {
    final bytes =
        value.codeUnits.map((unit) => unit & 0xff).toList(growable: false);
    return utf8.decode(bytes, allowMalformed: true);
  } catch (_) {
    return value;
  }
}
