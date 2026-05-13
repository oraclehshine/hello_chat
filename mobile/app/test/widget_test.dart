import 'package:app/app/hello_chat_app.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('renders mobile auth shell', (tester) async {
    SharedPreferences.setMockInitialValues({});
    await tester.pumpWidget(const HelloChatApp());
    await tester.pump(const Duration(milliseconds: 500));

    expect(find.text('登录 Hello Chat'), findsOneWidget);
    expect(find.text('创建移动端账号'), findsNothing);
    expect(find.text('进入工作台'), findsOneWidget);
  });
}
