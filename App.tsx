import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  FlatList,
  Alert,
  SafeAreaView,
  ScrollView,
  TextInput,
  Modal,
} from 'react-native';
import { StatusBar } from 'expo-status-bar';
import CameraScreen from './src/screens/CameraScreen';
import { Book } from './src/types';
import { exportToExcel } from './src/utils/excelExport';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function App() {
  const [books, setBooks] = useState<Book[]>([]);
  const [showCamera, setShowCamera] = useState(false);
  const [editingBook, setEditingBook] = useState<Book | null>(null);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editedTitle, setEditedTitle] = useState('');

  // 从本地存储加载书籍列表
  useEffect(() => {
    loadBooks();
  }, []);

  // 保存书籍列表到本地存储
  useEffect(() => {
    saveBooks();
  }, [books]);

  const loadBooks = async () => {
    try {
      const savedBooks = await AsyncStorage.getItem('books');
      if (savedBooks) {
        setBooks(JSON.parse(savedBooks));
      }
    } catch (error) {
      console.error('加载书籍列表失败:', error);
    }
  };

  const saveBooks = async () => {
    try {
      await AsyncStorage.setItem('books', JSON.stringify(books));
    } catch (error) {
      console.error('保存书籍列表失败:', error);
    }
  };

  // 添加新书籍
  const handleAddBook = (title: string, imageUri?: string) => {
    const newBook: Book = {
      id: Date.now().toString(),
      title,
      imageUri,
      timestamp: new Date().toISOString(),
    };
    setBooks([newBook, ...books]);
    setShowCamera(false);
  };

  // 删除书籍
  const handleDeleteBook = (id: string) => {
    Alert.alert(
      '确认删除',
      '确定要删除这本书吗？',
      [
        { text: '取消', style: 'cancel' },
        {
          text: '删除',
          style: 'destructive',
          onPress: () => {
            setBooks(books.filter(book => book.id !== id));
          },
        },
      ]
    );
  };

  // 编辑书籍
  const handleEditBook = (book: Book) => {
    setEditingBook(book);
    setEditedTitle(book.title);
    setEditModalVisible(true);
  };

  const saveEditedBook = () => {
    if (editingBook && editedTitle.trim()) {
      setBooks(books.map(book =>
        book.id === editingBook.id
          ? { ...book, title: editedTitle.trim() }
          : book
      ));
      setEditModalVisible(false);
      setEditingBook(null);
      setEditedTitle('');
    }
  };

  // 导出到Excel
  const handleExportToExcel = async () => {
    if (books.length === 0) {
      Alert.alert('提示', '没有可导出的书籍');
      return;
    }

    try {
      await exportToExcel(books);
      Alert.alert('成功', '书籍列表已导出到Excel文件');
    } catch (error) {
      Alert.alert('错误', '导出失败: ' + error);
    }
  };

  // 清空列表
  const handleClearAll = () => {
    Alert.alert(
      '确认清空',
      '确定要清空所有书籍吗？',
      [
        { text: '取消', style: 'cancel' },
        {
          text: '清空',
          style: 'destructive',
          onPress: () => setBooks([]),
        },
      ]
    );
  };

  if (showCamera) {
    return (
      <CameraScreen
        onCapture={handleAddBook}
        onClose={() => setShowCamera(false)}
      />
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar style="auto" />
      
      {/* 头部 */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>书籍扫描器</Text>
        <Text style={styles.headerSubtitle}>已扫描 {books.length} 本书</Text>
      </View>

      {/* 书籍列表 */}
      {books.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Text style={styles.emptyText}>📚</Text>
          <Text style={styles.emptyTitle}>还没有扫描任何书籍</Text>
          <Text style={styles.emptySubtitle}>点击下方按钮开始扫描</Text>
        </View>
      ) : (
        <FlatList
          data={books}
          keyExtractor={(item) => item.id}
          renderItem={({ item, index }) => (
            <View style={styles.bookItem}>
              <View style={styles.bookInfo}>
                <Text style={styles.bookNumber}>{index + 1}</Text>
                <View style={styles.bookDetails}>
                  <Text style={styles.bookTitle}>{item.title}</Text>
                  <Text style={styles.bookTime}>
                    {new Date(item.timestamp).toLocaleString('zh-CN')}
                  </Text>
                </View>
              </View>
              <View style={styles.bookActions}>
                <TouchableOpacity
                  style={styles.editButton}
                  onPress={() => handleEditBook(item)}
                >
                  <Text style={styles.editButtonText}>✏️</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.deleteButton}
                  onPress={() => handleDeleteBook(item.id)}
                >
                  <Text style={styles.deleteButtonText}>🗑️</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
          contentContainerStyle={styles.listContent}
        />
      )}

      {/* 底部按钮 */}
      <View style={styles.buttonContainer}>
        <TouchableOpacity
          style={[styles.button, styles.scanButton]}
          onPress={() => setShowCamera(true)}
        >
          <Text style={styles.buttonText}>📷 扫描书籍</Text>
        </TouchableOpacity>

        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.button, styles.exportButton, books.length === 0 && styles.disabledButton]}
            onPress={handleExportToExcel}
            disabled={books.length === 0}
          >
            <Text style={styles.buttonText}>📊 导出Excel</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.button, styles.clearButton, books.length === 0 && styles.disabledButton]}
            onPress={handleClearAll}
            disabled={books.length === 0}
          >
            <Text style={styles.buttonText}>🗑️ 清空</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* 编辑弹窗 */}
      <Modal
        visible={editModalVisible}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setEditModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>编辑书名</Text>
            <TextInput
              style={styles.modalInput}
              value={editedTitle}
              onChangeText={setEditedTitle}
              placeholder="请输入书名"
              autoFocus
            />
            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={[styles.modalButton, styles.modalCancelButton]}
                onPress={() => {
                  setEditModalVisible(false);
                  setEditingBook(null);
                  setEditedTitle('');
                }}
              >
                <Text style={styles.modalButtonText}>取消</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalButton, styles.modalSaveButton]}
                onPress={saveEditedBook}
              >
                <Text style={[styles.modalButtonText, styles.modalSaveButtonText]}>保存</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#4CAF50',
    padding: 20,
    paddingTop: 10,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: 'white',
    marginBottom: 5,
  },
  headerSubtitle: {
    fontSize: 14,
    color: 'rgba(255, 255, 255, 0.9)',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  emptyText: {
    fontSize: 80,
    marginBottom: 20,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  emptySubtitle: {
    fontSize: 16,
    color: '#666',
  },
  listContent: {
    padding: 15,
  },
  bookItem: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 15,
    marginBottom: 12,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  bookInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  bookNumber: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#4CAF50',
    marginRight: 15,
    width: 30,
  },
  bookDetails: {
    flex: 1,
  },
  bookTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  bookTime: {
    fontSize: 12,
    color: '#999',
  },
  bookActions: {
    flexDirection: 'row',
    gap: 8,
  },
  editButton: {
    padding: 8,
  },
  editButtonText: {
    fontSize: 20,
  },
  deleteButton: {
    padding: 8,
  },
  deleteButtonText: {
    fontSize: 20,
  },
  buttonContainer: {
    padding: 15,
    backgroundColor: 'white',
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
  },
  buttonRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 10,
  },
  button: {
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scanButton: {
    backgroundColor: '#4CAF50',
  },
  exportButton: {
    backgroundColor: '#2196F3',
    flex: 1,
  },
  clearButton: {
    backgroundColor: '#FF5722',
    flex: 1,
  },
  disabledButton: {
    backgroundColor: '#cccccc',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: 'white',
    borderRadius: 16,
    padding: 24,
    width: '85%',
    maxWidth: 400,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    color: '#333',
  },
  modalInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 20,
  },
  modalButtons: {
    flexDirection: 'row',
    gap: 12,
  },
  modalButton: {
    flex: 1,
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  modalCancelButton: {
    backgroundColor: '#f5f5f5',
  },
  modalSaveButton: {
    backgroundColor: '#4CAF50',
  },
  modalButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  modalSaveButtonText: {
    color: 'white',
  },
});

